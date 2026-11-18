import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Chess rules and game state. This class has no dependency on the user interface. */
public final class ChessGame {
    private static final int[][] KNIGHT_OFFSETS = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };
    private static final int[][] BISHOP_DIRECTIONS = {
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };
    private static final int[][] ROOK_DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };
    private static final int[][] QUEEN_DIRECTIONS = {
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1},
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };
    private static final int[] PAWN_CAPTURE_OFFSETS = {-1, 1};
    private static final int[] ROOK_HOME_COLUMNS = {0, 7};

    public enum Color {
        WHITE, BLACK;

        public Color opposite() {
            return this == WHITE ? BLACK : WHITE;
        }
    }

    public enum Type { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }

    public static final class Piece {
        public final Type type;
        public final Color color;
        private boolean moved;

        private Piece(Type type, Color color) {
            this.type = type;
            this.color = color;
        }
    }

    public static final class Move {
        public final int fromRow;
        public final int fromCol;
        public final int toRow;
        public final int toCol;

        private Move(int fromRow, int fromCol, int toRow, int toCol) {
            this.fromRow = fromRow;
            this.fromCol = fromCol;
            this.toRow = toRow;
            this.toCol = toCol;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Move)) return false;
            Move move = (Move) other;
            return fromRow == move.fromRow && fromCol == move.fromCol
                    && toRow == move.toRow && toCol == move.toCol;
        }

        @Override
        public int hashCode() {
            return (((fromRow * 8 + fromCol) * 8 + toRow) * 8 + toCol);
        }
    }

    private final Piece[][] board = new Piece[8][8];
    private Color turn = Color.WHITE;
    private int enPassantRow = -1;
    private int enPassantCol = -1;
    private Move lastMove;
    private int whiteKingRow;
    private int whiteKingCol;
    private int blackKingRow;
    private int blackKingCol;

    public ChessGame() {
        this(true);
    }

    private ChessGame(boolean initialize) {
        if (initialize) reset();
    }

    public void reset() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) board[row][col] = null;
        }
        Type[] backRank = {Type.ROOK, Type.KNIGHT, Type.BISHOP, Type.QUEEN,
                Type.KING, Type.BISHOP, Type.KNIGHT, Type.ROOK};
        for (int col = 0; col < 8; col++) {
            board[0][col] = new Piece(backRank[col], Color.BLACK);
            board[1][col] = new Piece(Type.PAWN, Color.BLACK);
            board[6][col] = new Piece(Type.PAWN, Color.WHITE);
            board[7][col] = new Piece(backRank[col], Color.WHITE);
        }
        turn = Color.WHITE;
        enPassantRow = enPassantCol = -1;
        lastMove = null;
        whiteKingRow = 7;
        whiteKingCol = 4;
        blackKingRow = 0;
        blackKingCol = 4;
    }

    /** Creates an independent snapshot for bot search without changing the live game. */
    public ChessGame copy() {
        ChessGame copy = new ChessGame(false);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null) {
                    Piece clone = new Piece(piece.type, piece.color);
                    clone.moved = piece.moved;
                    copy.board[row][col] = clone;
                } else {
                    copy.board[row][col] = null;
                }
            }
        }
        copy.turn = turn;
        copy.enPassantRow = enPassantRow;
        copy.enPassantCol = enPassantCol;
        copy.lastMove = lastMove;
        copy.whiteKingRow = whiteKingRow;
        copy.whiteKingCol = whiteKingCol;
        copy.blackKingRow = blackKingRow;
        copy.blackKingCol = blackKingCol;
        return copy;
    }

    /** Board rows count down from rank eight; columns run from file a to h. */
    public Piece pieceAt(int row, int col) {
        return inside(row, col) ? board[row][col] : null;
    }

    public Color currentTurn() {
        return turn;
    }

    public Move lastMove() {
        return lastMove;
    }

    public List<Move> legalMovesFrom(int row, int col) {
        if (!inside(row, col) || board[row][col] == null || board[row][col].color != turn) {
            return Collections.emptyList();
        }
        List<Move> legal = new ArrayList<>();
        for (Move move : pseudoMoves(row, col)) {
            if (!wouldLeaveKingInCheck(move)) legal.add(move);
        }
        return legal;
    }

    public boolean isInCheck(Color color) {
        int row = color == Color.WHITE ? whiteKingRow : blackKingRow;
        int col = color == Color.WHITE ? whiteKingCol : blackKingCol;
        return isSquareAttacked(row, col, color.opposite());
    }

    public boolean isCheckmate() {
        return isInCheck(turn) && !hasLegalMoves();
    }

    public boolean isStalemate() {
        return !isInCheck(turn) && !hasLegalMoves();
    }

    /** Returns whether the side to move has at least one legal move. */
    public boolean hasLegalMoves() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.color == turn && !legalMovesFrom(row, col).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Applies a legal move. Promotion defaults to a queen when no choice is supplied. */
    public boolean move(int fromRow, int fromCol, int toRow, int toCol, Type promotion) {
        Move move = new Move(fromRow, fromCol, toRow, toCol);
        if (!legalMovesFrom(fromRow, fromCol).contains(move)) return false;
        applyMove(move, promotion);
        return true;
    }

    /** Applies a move returned by legal move generation without generating it again. */
    void applyGeneratedLegalMove(Move move) {
        applyMove(move, Type.QUEEN);
    }

    private void applyMove(Move move, Type promotion) {
        int fromRow = move.fromRow;
        int fromCol = move.fromCol;
        int toRow = move.toRow;
        int toCol = move.toCol;
        Piece moving = board[fromRow][fromCol];
        Piece captured = board[toRow][toCol];

        // En passant removes the pawn beside the moving pawn, not a piece on the destination.
        if (moving.type == Type.PAWN && toRow == enPassantRow && toCol == enPassantCol
                && captured == null && fromCol != toCol) {
            captured = board[fromRow][toCol];
            board[fromRow][toCol] = null;
        }

        board[fromRow][fromCol] = null;
        board[toRow][toCol] = moving;

        // Castling also moves the rook beside the king.
        if (moving.type == Type.KING && Math.abs(toCol - fromCol) == 2) {
            int rookFrom = toCol > fromCol ? 7 : 0;
            int rookTo = toCol > fromCol ? toCol - 1 : toCol + 1;
            board[toRow][rookTo] = board[toRow][rookFrom];
            board[toRow][rookFrom] = null;
            board[toRow][rookTo].moved = true;
        }

        moving.moved = true;
        if (moving.type == Type.KING) setKingPosition(moving.color, toRow, toCol);
        if (moving.type == Type.PAWN && (toRow == 0 || toRow == 7)) {
            Type chosen = promotion == null ? Type.QUEEN : promotion;
            if (chosen == Type.PAWN || chosen == Type.KING) chosen = Type.QUEEN;
            board[toRow][toCol] = new Piece(chosen, moving.color);
        }

        enPassantRow = enPassantCol = -1;
        if (moving.type == Type.PAWN && Math.abs(toRow - fromRow) == 2) {
            enPassantRow = (toRow + fromRow) / 2;
            enPassantCol = fromCol;
        }
        lastMove = move;
        turn = turn.opposite();
    }

    private List<Move> pseudoMoves(int row, int col) {
        List<Move> moves = new ArrayList<>();
        Piece piece = board[row][col];
        switch (piece.type) {
            case PAWN: addPawnMoves(moves, row, col, piece); break;
            case KNIGHT:
                for (int[] jump : KNIGHT_OFFSETS) {
                    addIfAvailable(moves, row, col, row + jump[0], col + jump[1], piece.color);
                }
                break;
            case BISHOP: addSlidingMoves(moves, row, col, piece.color, BISHOP_DIRECTIONS); break;
            case ROOK: addSlidingMoves(moves, row, col, piece.color, ROOK_DIRECTIONS); break;
            case QUEEN: addSlidingMoves(moves, row, col, piece.color, QUEEN_DIRECTIONS); break;
            case KING:
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr != 0 || dc != 0) addIfAvailable(moves, row, col, row + dr, col + dc, piece.color);
                    }
                }
                addCastlingMoves(moves, row, col, piece);
                break;
            default: break;
        }
        return moves;
    }

    private void addPawnMoves(List<Move> moves, int row, int col, Piece pawn) {
        int direction = pawn.color == Color.WHITE ? -1 : 1;
        int nextRow = row + direction;
        if (inside(nextRow, col) && board[nextRow][col] == null) {
            moves.add(new Move(row, col, nextRow, col));
            int startRow = pawn.color == Color.WHITE ? 6 : 1;
            if (row == startRow && board[row + 2 * direction][col] == null) {
                moves.add(new Move(row, col, row + 2 * direction, col));
            }
        }
        for (int dc : PAWN_CAPTURE_OFFSETS) {
            int targetCol = col + dc;
            if (!inside(nextRow, targetCol)) continue;
            Piece target = board[nextRow][targetCol];
            if (target != null && target.color != pawn.color && target.type != Type.KING) {
                moves.add(new Move(row, col, nextRow, targetCol));
            } else if (nextRow == enPassantRow && targetCol == enPassantCol) {
                Piece adjacent = board[row][targetCol];
                if (adjacent != null && adjacent.type == Type.PAWN && adjacent.color != pawn.color) {
                    moves.add(new Move(row, col, nextRow, targetCol));
                }
            }
        }
    }

    private void addSlidingMoves(List<Move> moves, int row, int col, Color color, int[][] directions) {
        for (int[] direction : directions) {
            int r = row + direction[0];
            int c = col + direction[1];
            while (inside(r, c)) {
                Piece target = board[r][c];
                if (target == null) {
                    moves.add(new Move(row, col, r, c));
                } else {
                    if (target.color != color && target.type != Type.KING) moves.add(new Move(row, col, r, c));
                    break;
                }
                r += direction[0];
                c += direction[1];
            }
        }
    }

    private void addIfAvailable(List<Move> moves, int fromRow, int fromCol, int row, int col, Color color) {
        if (!inside(row, col)) return;
        Piece target = board[row][col];
        if (target == null || (target.color != color && target.type != Type.KING)) {
            moves.add(new Move(fromRow, fromCol, row, col));
        }
    }

    private void addCastlingMoves(List<Move> moves, int row, int col, Piece king) {
        if (king.moved || isSquareAttacked(row, col, king.color.opposite())) return;
        for (int rookCol : ROOK_HOME_COLUMNS) {
            Piece rook = board[row][rookCol];
            if (rook == null || rook.type != Type.ROOK || rook.color != king.color || rook.moved) continue;
            int step = rookCol < col ? -1 : 1;
            boolean clear = true;
            for (int c = col + step; c != rookCol; c += step) {
                if (board[row][c] != null) { clear = false; break; }
            }
            if (!clear) continue;
            int transitCol = col + step;
            int landingCol = col + 2 * step;
            if (!isSquareAttacked(row, transitCol, king.color.opposite())
                    && !isSquareAttacked(row, landingCol, king.color.opposite())) {
                moves.add(new Move(row, col, row, landingCol));
            }
        }
    }

    private boolean wouldLeaveKingInCheck(Move move) {
        Piece moving = board[move.fromRow][move.fromCol];
        Piece captured = board[move.toRow][move.toCol];
        int epRow = -1, epCol = -1;
        if (moving.type == Type.PAWN && move.toRow == enPassantRow && move.toCol == enPassantCol
                && captured == null && move.fromCol != move.toCol) {
            epRow = move.fromRow;
            epCol = move.toCol;
            captured = board[epRow][epCol];
            board[epRow][epCol] = null;
        }
        board[move.fromRow][move.fromCol] = null;
        board[move.toRow][move.toCol] = moving;
        if (moving.type == Type.KING) setKingPosition(moving.color, move.toRow, move.toCol);
        int rookFrom = -1, rookTo = -1;
        if (moving.type == Type.KING && Math.abs(move.toCol - move.fromCol) == 2) {
            rookFrom = move.toCol > move.fromCol ? 7 : 0;
            rookTo = move.toCol > move.fromCol ? move.toCol - 1 : move.toCol + 1;
            board[move.toRow][rookTo] = board[move.toRow][rookFrom];
            board[move.toRow][rookFrom] = null;
        }
        boolean inCheck = isInCheck(moving.color);
        if (rookFrom >= 0) {
            board[move.toRow][rookFrom] = board[move.toRow][rookTo];
            board[move.toRow][rookTo] = null;
        }
        board[move.fromRow][move.fromCol] = moving;
        board[move.toRow][move.toCol] = epRow >= 0 ? null : captured;
        if (epRow >= 0) board[epRow][epCol] = captured;
        if (moving.type == Type.KING) setKingPosition(moving.color, move.fromRow, move.fromCol);
        return inCheck;
    }

    private boolean isSquareAttacked(int row, int col, Color byColor) {
        int pawnDirection = byColor == Color.WHITE ? -1 : 1;
        int pawnRow = row - pawnDirection;
        for (int dc : PAWN_CAPTURE_OFFSETS) {
            int pawnCol = col + dc;
            if (inside(pawnRow, pawnCol) && isPiece(pawnRow, pawnCol, byColor, Type.PAWN)) return true;
        }
        for (int[] jump : KNIGHT_OFFSETS) {
            if (isPiece(row + jump[0], col + jump[1], byColor, Type.KNIGHT)) return true;
        }
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if ((dr != 0 || dc != 0) && isPiece(row + dr, col + dc, byColor, Type.KING)) return true;
            }
        }
        return rayAttacked(row, col, byColor, BISHOP_DIRECTIONS, Type.BISHOP)
                || rayAttacked(row, col, byColor, ROOK_DIRECTIONS, Type.ROOK);
    }

    private boolean rayAttacked(int row, int col, Color color, int[][] directions, Type slider) {
        for (int[] direction : directions) {
            int r = row + direction[0], c = col + direction[1];
            while (inside(r, c)) {
                Piece piece = board[r][c];
                if (piece != null) {
                    if (piece.color == color && (piece.type == slider || piece.type == Type.QUEEN)) return true;
                    break;
                }
                r += direction[0];
                c += direction[1];
            }
        }
        return false;
    }

    private boolean isPiece(int row, int col, Color color, Type type) {
        return inside(row, col) && board[row][col] != null
                && board[row][col].color == color && board[row][col].type == type;
    }

    private void setKingPosition(Color color, int row, int col) {
        if (color == Color.WHITE) {
            whiteKingRow = row;
            whiteKingCol = col;
        } else {
            blackKingRow = row;
            blackKingCol = col;
        }
    }

    private boolean inside(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
}
