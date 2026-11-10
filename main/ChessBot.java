import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Selects a legal move for the computer opponent. */
public final class ChessBot {
    private static final Random RANDOM = new Random();

    private ChessBot() { }

    public static ChessGame.Move chooseMove(ChessGame game, boolean thoughtful) {
        List<ChessGame.Move> moves = legalMoves(game);
        if (moves.isEmpty()) return null;
        if (!thoughtful) return moves.get(RANDOM.nextInt(moves.size()));

        ChessGame.Color botColor = game.currentTurn();
        ChessGame.Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        for (ChessGame.Move move : moves) {
            if (Thread.currentThread().isInterrupted()) break;
            ChessGame next = game.copy();
            next.move(move.fromRow, move.fromCol, move.toRow, move.toCol, ChessGame.Type.QUEEN);
            int score = search(next, 1, Integer.MIN_VALUE, Integer.MAX_VALUE, botColor);
            if (score > bestScore || (score == bestScore && RANDOM.nextBoolean())) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private static int search(ChessGame game, int depth, int alpha, int beta, ChessGame.Color botColor) {
        if (depth == 0) {
            if (game.isInCheck(game.currentTurn()) && legalMoves(game).isEmpty()) {
                return game.currentTurn() == botColor ? -100000 : 100000;
            }
            return evaluate(game, botColor);
        }
        List<ChessGame.Move> moves = legalMoves(game);
        if (moves.isEmpty()) {
            if (game.isInCheck(game.currentTurn())) {
                return game.currentTurn() == botColor ? -100000 - depth : 100000 + depth;
            }
            return 0;
        }
        boolean maximizing = game.currentTurn() == botColor;
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (ChessGame.Move move : moves) {
            if (Thread.currentThread().isInterrupted()) break;
            ChessGame next = game.copy();
            next.move(move.fromRow, move.fromCol, move.toRow, move.toCol, ChessGame.Type.QUEEN);
            int score = search(next, depth - 1, alpha, beta, botColor);
            if (maximizing) {
                best = Math.max(best, score);
                alpha = Math.max(alpha, best);
            } else {
                best = Math.min(best, score);
                beta = Math.min(beta, best);
            }
            if (beta <= alpha) break;
        }
        return best;
    }

    private static int evaluate(ChessGame game, ChessGame.Color perspective) {
        int score = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessGame.Piece piece = game.pieceAt(row, col);
                if (piece == null) continue;
                int value = value(piece.type);
                int centerDistance = Math.abs(3 - row) + Math.abs(3 - col);
                if (piece.type == ChessGame.Type.KNIGHT || piece.type == ChessGame.Type.BISHOP) {
                    value += (6 - centerDistance) * 4;
                } else if (piece.type == ChessGame.Type.PAWN) {
                    int advance = piece.color == ChessGame.Color.WHITE ? 6 - row : row - 1;
                    value += advance * 5;
                }
                score += piece.color == perspective ? value : -value;
            }
        }
        if (game.isInCheck(perspective)) score -= 24;
        if (game.isInCheck(perspective.opposite())) score += 24;
        return score;
    }

    private static int value(ChessGame.Type type) {
        switch (type) {
            case QUEEN: return 900;
            case ROOK: return 500;
            case BISHOP: return 330;
            case KNIGHT: return 320;
            case PAWN: return 100;
            default: return 0;
        }
    }

    private static List<ChessGame.Move> legalMoves(ChessGame game) {
        List<ChessGame.Move> moves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessGame.Piece piece = game.pieceAt(row, col);
                if (piece != null && piece.color == game.currentTurn()) {
                    moves.addAll(game.legalMovesFrom(row, col));
                }
            }
        }
        return moves;
    }
}
