import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/** A themed chess UI that presents and controls the separate ChessGame rules engine. */
public final class ChessBoardPanel extends JPanel {
    private static final Color BACKGROUND = new Color(19, 27, 28);
    private static final Color SURFACE = new Color(31, 41, 41);
    private static final Color SURFACE_ACTIVE = new Color(41, 54, 51);
    private static final Color TEXT = new Color(242, 239, 226);
    private static final Color MUTED = new Color(156, 167, 157);
    private static final Color ACCENT = new Color(214, 177, 104);
    private static final Color SQUARE_LIGHT = new Color(231, 222, 196);
    private static final Color SQUARE_DARK = new Color(77, 112, 99);
    private static final Color MOVE_MARK = new Color(203, 183, 107, 165);
    private static final int ANIMATION_MS = 210;

    private final ChessGame game = new ChessGame();
    private final BoardCanvas board = new BoardCanvas();
    private final JLabel whiteCard = new JLabel();
    private final JLabel blackCard = new JLabel();
    private final JLabel status = new JLabel("White to move");
    private final JButton motionButton = new ThemedButton("Motion: On");
    private final JComboBox<String> opponentChoice = new JComboBox<>(new String[]{"Two players", "Play the computer"});
    private final JComboBox<String> colorChoice = new JComboBox<>(new String[]{"I play White", "I play Black"});
    private final JComboBox<String> difficultyChoice = new JComboBox<>(new String[]{"Casual", "Thoughtful"});
    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<ChessGame.Move> selectedMoves = new ArrayList<>();
    private boolean motionEnabled = true;
    private long animationStarted;
    private Point animationFrom;
    private Point animationTo;
    private ChessGame.Piece animatedPiece;
    private Timer animationTimer;
    private Timer botTimer;
    private SwingWorker<ChessGame.Move, Void> botWorker;
    private boolean botThinking;
    private boolean currentTurnInCheck;
    private int pressedRow = -1;
    private int pressedCol = -1;
    private Point pressPoint;
    private Point dragPoint;
    private ChessGame.Piece draggedPiece;
    private boolean dragging;
    private boolean suppressNextClick;

    public ChessBoardPanel() {
        super(new BorderLayout());
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel brand = new JLabel("SIMPLE CHESS");
        brand.setFont(new Font("SansSerif", Font.BOLD, 13));
        brand.setForeground(ACCENT);
        JLabel tagline = new JLabel("A quiet match, one move at a time");
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tagline.setForeground(MUTED);
        JPanel title = new JPanel();
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.setOpaque(false);
        title.add(brand);
        title.add(Box.createVerticalStrut(5));
        title.add(tagline);
        header.add(title, BorderLayout.WEST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 2, 18, 0));

        JPanel content = new JPanel(new BorderLayout(24, 0));
        content.setOpaque(false);
        JPanel boardFrame = new JPanel(new BorderLayout());
        boardFrame.setOpaque(false);
        boardFrame.add(board, BorderLayout.CENTER);
        content.add(boardFrame, BorderLayout.CENTER);
        content.add(buildSidebar(), BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(246, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        JLabel gameLabel = new JLabel("THE MATCH");
        gameLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        gameLabel.setForeground(MUTED);
        gameLabel.setAlignmentX(LEFT_ALIGNMENT);
        side.add(Box.createVerticalStrut(5));
        side.add(gameLabel);
        side.add(Box.createVerticalStrut(13));
        addChoice(side, "OPPONENT", opponentChoice);
        side.add(Box.createVerticalStrut(10));
        addChoice(side, "YOUR COLOR", colorChoice);
        side.add(Box.createVerticalStrut(10));
        addChoice(side, "BOT STYLE", difficultyChoice);
        opponentChoice.addActionListener(event -> updateGameChoices());
        colorChoice.addActionListener(event -> updateGameChoices());
        difficultyChoice.addActionListener(event -> updateGameChoices());
        colorChoice.setEnabled(false);
        difficultyChoice.setEnabled(false);
        side.add(Box.createVerticalStrut(16));
        stylePlayerCard(whiteCard, "\u2654", "WHITE", "Player one");
        stylePlayerCard(blackCard, "\u265A", "BLACK", "Player two");
        side.add(whiteCard);
        side.add(Box.createVerticalStrut(9));
        side.add(blackCard);
        side.add(Box.createVerticalStrut(18));

        JPanel statusBox = new JPanel();
        statusBox.setLayout(new BoxLayout(statusBox, BoxLayout.Y_AXIS));
        statusBox.setBackground(SURFACE);
        statusBox.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        statusBox.setAlignmentX(LEFT_ALIGNMENT);
        statusBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        JLabel statusLabel = new JLabel("GAME STATUS");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        statusLabel.setForeground(MUTED);
        status.setFont(new Font("SansSerif", Font.BOLD, 16));
        status.setForeground(TEXT);
        statusBox.add(statusLabel);
        statusBox.add(Box.createVerticalStrut(7));
        statusBox.add(status);
        side.add(statusBox);
        side.add(Box.createVerticalStrut(22));

        JLabel howTo = new JLabel("YOUR TURN");
        howTo.setFont(new Font("SansSerif", Font.BOLD, 11));
        howTo.setForeground(ACCENT);
        howTo.setAlignmentX(LEFT_ALIGNMENT);
        side.add(howTo);
        side.add(Box.createVerticalStrut(9));
        JLabel instructions = new JLabel("<html><div style='width:205px; line-height:1.55'>"
                + "Drag a piece to a highlighted square, or select it and click a destination."
                + "</div></html>");
        instructions.setFont(new Font("SansSerif", Font.PLAIN, 13));
        instructions.setForeground(MUTED);
        instructions.setAlignmentX(LEFT_ALIGNMENT);
        side.add(instructions);

        side.add(Box.createVerticalGlue());
        JButton newGame = new ThemedButton("New game");
        styleButton(newGame, true);
        newGame.setAlignmentX(LEFT_ALIGNMENT);
        newGame.addActionListener(event -> startNewGame());
        side.add(newGame);
        side.add(Box.createVerticalStrut(9));
        styleButton(motionButton, false);
        motionButton.setAlignmentX(LEFT_ALIGNMENT);
        motionButton.addActionListener(event -> {
            motionEnabled = !motionEnabled;
            motionButton.setText(motionEnabled ? "Motion: On" : "Motion: Off");
            if (!motionEnabled) stopAnimation();
        });
        side.add(motionButton);
        return side;
    }

    private void addChoice(JPanel parent, String title, JComboBox<String> choice) {
        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(MUTED);
        label.setAlignmentX(LEFT_ALIGNMENT);
        choice.setFont(new Font("SansSerif", Font.PLAIN, 12));
        choice.setForeground(BACKGROUND);
        choice.setBackground(SURFACE);
        choice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        choice.setAlignmentX(LEFT_ALIGNMENT);
        parent.add(label);
        parent.add(Box.createVerticalStrut(5));
        parent.add(choice);
    }

    private void updateGameChoices() {
        boolean botGame = isBotGame();
        colorChoice.setEnabled(botGame);
        difficultyChoice.setEnabled(botGame);
        startNewGame();
    }

    private boolean isBotGame() {
        return opponentChoice.getSelectedIndex() == 1;
    }

    private ChessGame.Color botColor() {
        return colorChoice.getSelectedIndex() == 0 ? ChessGame.Color.BLACK : ChessGame.Color.WHITE;
    }

    private boolean isHumanTurn() {
        return !isBotGame() || game.currentTurn() != botColor();
    }

    private void startNewGame() {
        stopBotSearch();
        stopAnimation();
        game.reset();
        clearSelection();
        refresh();
        scheduleBotMove();
    }

    private void stylePlayerCard(JLabel card, String icon, String name, String player) {
        card.setText("<html><div style='padding:2px 0'><span style='font-size:27px;color:#d6b168'>"
                + icon + "</span>&nbsp;&nbsp;<b style='font-size:12px;letter-spacing:1px'>" + name
                + "</b><br><span style='color:#9ca79d;font-size:11px'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                + player + "</span></div></html>");
        card.setFont(new Font("SansSerif", Font.PLAIN, 12));
        card.setForeground(TEXT);
        card.setOpaque(true);
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE, 1), BorderFactory.createEmptyBorder(7, 13, 7, 10)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
    }

    private void styleButton(JButton button, boolean primary) {
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(primary ? BACKGROUND : TEXT);
        button.setBackground(primary ? ACCENT : SURFACE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        button.setRolloverEnabled(true);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    /** Paints a stable high-contrast button without platform theme color overrides. */
    private static final class ThemedButton extends JButton {
        ThemedButton(String text) {
            super(text);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = getBackground();
            if (getModel().isPressed()) fill = fill.darker();
            else if (getModel().isRollover()) fill = fill.brighter();
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private void clickSquare(int row, int col) {
        if (animationTimer != null || !isHumanTurn()) return;
        if (selectedRow >= 0) {
            for (ChessGame.Move move : selectedMoves) {
                if (move.toRow == row && move.toCol == col) {
                    int fromRow = selectedRow;
                    int fromCol = selectedCol;
                    attemptMove(fromRow, fromCol, row, col, true);
                    return;
                }
            }
        }
        ChessGame.Piece piece = game.pieceAt(row, col);
        if (piece != null && piece.color == game.currentTurn()) {
            selectedRow = row;
            selectedCol = col;
            selectedMoves = game.legalMovesFrom(row, col);
        } else {
            clearSelection();
        }
        board.repaint();
    }

    private void attemptMove(int fromRow, int fromCol, int toRow, int toCol) {
        attemptMove(fromRow, fromCol, toRow, toCol, true);
    }

    private void attemptMove(int fromRow, int fromCol, int toRow, int toCol, boolean animate) {
        ChessGame.Piece moving = game.pieceAt(fromRow, fromCol);
        if (moving == null) return;
        ChessGame.Type promotion = null;
        if (moving.type == ChessGame.Type.PAWN && (toRow == 0 || toRow == 7)) {
            promotion = choosePromotion();
        }
        if (game.move(fromRow, fromCol, toRow, toCol, promotion) && motionEnabled && animate) {
            startAnimation(moving, fromRow, fromCol, toRow, toCol);
        }
        clearSelection();
        refresh();
        scheduleBotMove();
    }

    private ChessGame.Type choosePromotion() {
        ChessGame.Type[] choices = {ChessGame.Type.QUEEN, ChessGame.Type.ROOK,
                ChessGame.Type.BISHOP, ChessGame.Type.KNIGHT};
        Object choice = javax.swing.JOptionPane.showInputDialog(this, "Choose your promotion piece",
                "Pawn promotion", javax.swing.JOptionPane.PLAIN_MESSAGE, null, choices, ChessGame.Type.QUEEN);
        return choice == null ? ChessGame.Type.QUEEN : (ChessGame.Type) choice;
    }

    private void startAnimation(ChessGame.Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        animationFrom = board.squareCenter(fromRow, fromCol);
        animationTo = board.squareCenter(toRow, toCol);
        animatedPiece = piece;
        animationStarted = System.nanoTime();
        animationTimer = new Timer(1000 / 60, event -> {
            if ((System.nanoTime() - animationStarted) / 1_000_000 >= ANIMATION_MS) {
                stopAnimation();
                refresh();
            } else {
                board.repaint();
            }
        });
        animationTimer.setCoalesce(true);
        animationTimer.start();
    }

    private void scheduleBotMove() {
        if (!isBotGame() || isHumanTurn() || !game.hasLegalMoves()) return;
        botThinking = true;
        refresh();
        botTimer = new Timer(450, event -> {
            botTimer.stop();
            botTimer = null;
            boolean thoughtful = difficultyChoice.getSelectedIndex() == 1;
            ChessGame position = game.copy();
            botWorker = new SwingWorker<ChessGame.Move, Void>() {
                @Override
                protected ChessGame.Move doInBackground() {
                    return ChessBot.chooseMove(position, thoughtful);
                }

                @Override
                protected void done() {
                    if (isCancelled()) return;
                    ChessGame.Move move = null;
                    try {
                        move = get();
                    } catch (Exception ignored) {
                        // Keep the game usable if background search is interrupted.
                    }
                    botThinking = false;
                    botWorker = null;
                    if (move != null && isBotGame() && !isHumanTurn()
                            && game.move(move.fromRow, move.fromCol, move.toRow, move.toCol, ChessGame.Type.QUEEN)) {
                        if (motionEnabled) startAnimation(game.pieceAt(move.toRow, move.toCol),
                                move.fromRow, move.fromCol, move.toRow, move.toCol);
                    }
                    refresh();
                }
            };
            botWorker.execute();
        });
        botTimer.setRepeats(false);
        botTimer.start();
    }

    private void stopBotSearch() {
        if (botTimer != null) {
            botTimer.stop();
            botTimer = null;
        }
        if (botWorker != null) {
            botWorker.cancel(true);
            botWorker = null;
        }
        botThinking = false;
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
        animatedPiece = null;
        if (board != null) board.repaint();
    }

    private void clearSelection() {
        selectedRow = selectedCol = -1;
        selectedMoves = new ArrayList<>();
    }

    private void refresh() {
        ChessGame.Color turn = game.currentTurn();
        currentTurnInCheck = game.isInCheck(turn);
        boolean hasLegalMoves = game.hasLegalMoves();
        boolean terminal = !hasLegalMoves;
        whiteCard.setText(playerCardText("\u2654", "WHITE", isBotGame()
                ? (botColor() == ChessGame.Color.WHITE ? "Computer" : "You") : "Player one"));
        blackCard.setText(playerCardText("\u265A", "BLACK", isBotGame()
                ? (botColor() == ChessGame.Color.BLACK ? "Computer" : "You") : "Player two"));
        whiteCard.setBackground(!terminal && turn == ChessGame.Color.WHITE ? SURFACE_ACTIVE : SURFACE);
        blackCard.setBackground(!terminal && turn == ChessGame.Color.BLACK ? SURFACE_ACTIVE : SURFACE);
        whiteCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(turn == ChessGame.Color.WHITE && !terminal ? ACCENT : SURFACE, 1),
                BorderFactory.createEmptyBorder(7, 13, 7, 10)));
        blackCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(turn == ChessGame.Color.BLACK && !terminal ? ACCENT : SURFACE, 1),
                BorderFactory.createEmptyBorder(7, 13, 7, 10)));
        String side = turn == ChessGame.Color.WHITE ? "White" : "Black";
        if (terminal && currentTurnInCheck) {
            status.setText("Checkmate");
            status.setForeground(ACCENT);
        } else if (terminal) {
            status.setText("Draw by stalemate");
            status.setForeground(MUTED);
        } else if (currentTurnInCheck) {
            status.setText(side + " is in check");
            status.setForeground(new Color(236, 142, 119));
        } else if (botThinking) {
            status.setText("Computer is thinking...");
            status.setForeground(ACCENT);
        } else {
            status.setText(side + " to move");
            status.setForeground(TEXT);
        }
        revalidate();
        board.repaint();
    }

    private String playerCardText(String icon, String name, String player) {
        return "<html><div style='padding:2px 0'><span style='font-size:27px;color:#d6b168'>"
                + icon + "</span>&nbsp;&nbsp;<b style='font-size:12px;letter-spacing:1px'>" + name
                + "</b><br><span style='color:#9ca79d;font-size:11px'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                + player + "</span></div></html>";
    }

    private final class BoardCanvas extends JPanel {
        private static final int GUTTER = 24;
        private int boardX;
        private int boardY;
        private int boardSize;
        private int squareSize;
        private Font pieceFont;
        private int pieceFontSize;

        BoardCanvas() {
            setOpaque(false);
            setPreferredSize(new Dimension(660, 660));
            getAccessibleContext().setAccessibleName("Chessboard");
            getAccessibleContext().setAccessibleDescription("Drag one of your pieces to a highlighted legal destination, or click a piece and then a destination.");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    int[] square = squareAt(event.getPoint());
                    if (square == null || animationTimer != null || !isHumanTurn()) return;
                    ChessGame.Piece piece = game.pieceAt(square[0], square[1]);
                    if (piece == null || piece.color != game.currentTurn()) return;
                    pressedRow = square[0];
                    pressedCol = square[1];
                    pressPoint = event.getPoint();
                    draggedPiece = piece;
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    // Some desktop/window-manager combinations omit MOUSE_DRAGGED;
                    // recognize a drag from the press/release distance as a fallback.
                    if (!dragging && draggedPiece != null && pressPoint != null
                            && pressPoint.distance(event.getPoint()) >= 5) {
                        beginDragging();
                    }
                    if (dragging) {
                        int[] destination = squareAt(event.getPoint());
                        boolean legal = false;
                        if (destination != null) {
                            for (ChessGame.Move move : selectedMoves) {
                                if (move.toRow == destination[0] && move.toCol == destination[1]) {
                                    legal = true;
                                    break;
                                }
                            }
                        }
                        if (legal) attemptMove(pressedRow, pressedCol, destination[0], destination[1], false);
                        else {
                            clearSelection();
                            repaint();
                        }
                        suppressNextClick = true;
                        Timer clearSuppression = new Timer(250, timerEvent -> suppressNextClick = false);
                        clearSuppression.setRepeats(false);
                        clearSuppression.start();
                    }
                    pressedRow = pressedCol = -1;
                    pressPoint = null;
                    dragPoint = null;
                    draggedPiece = null;
                    dragging = false;
                    setCursor(java.awt.Cursor.getDefaultCursor());
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    if (suppressNextClick) {
                        suppressNextClick = false;
                        return;
                    }
                    int[] square = squareAt(event.getPoint());
                    if (square != null) clickSquare(square[0], square[1]);
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent event) {
                    if (draggedPiece == null || pressPoint == null) return;
                    if (!dragging && pressPoint.distance(event.getPoint()) >= 5) {
                        beginDragging();
                    }
                    if (dragging) {
                        dragPoint = event.getPoint();
                        repaint();
                    }
                }
            });
        }

        private void beginDragging() {
            dragging = true;
            selectedRow = pressedRow;
            selectedCol = pressedCol;
            selectedMoves = game.legalMovesFrom(pressedRow, pressedCol);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));
        }

        private int[] squareAt(Point point) {
            updateBoardGeometry();
            if (squareSize <= 0 || point.x < boardX || point.y < boardY
                    || point.x >= boardX + boardSize || point.y >= boardY + boardSize) return null;
            return new int[]{(point.y - boardY) / squareSize, (point.x - boardX) / squareSize};
        }

        Point squareCenter(int row, int col) {
            updateBoardGeometry();
            return new Point(boardX + col * squareSize + squareSize / 2,
                    boardY + row * squareSize + squareSize / 2);
        }

        private void updateBoardGeometry() {
            int available = Math.min(getWidth(), getHeight()) - GUTTER * 2;
            boardSize = Math.max(8, available);
            squareSize = boardSize / 8;
            boardSize = squareSize * 8;
            boardX = (getWidth() - boardSize) / 2;
            boardY = (getHeight() - boardSize) / 2;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            updateBoardGeometry();
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRoundRect(boardX - 5, boardY - 5, boardSize + 10, boardSize + 10, 12, 12);

            ChessGame.Move last = game.lastMove();
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    int x = boardX + col * squareSize;
                    int y = boardY + row * squareSize;
                    boolean light = (row + col) % 2 == 0;
                    g.setColor(light ? SQUARE_LIGHT : SQUARE_DARK);
                    g.fillRect(x, y, squareSize, squareSize);
                    if (last != null && ((row == last.fromRow && col == last.fromCol)
                            || (row == last.toRow && col == last.toCol))) {
                        g.setColor(MOVE_MARK);
                        g.fillRect(x, y, squareSize, squareSize);
                    }
                    if (row == selectedRow && col == selectedCol) {
                        g.setColor(new Color(221, 177, 86, 135));
                        g.fillRect(x, y, squareSize, squareSize);
                    }
                    if (isLegalDestination(row, col)) drawMoveHint(g, row, col);
                    ChessGame.Piece piece = game.pieceAt(row, col);
                    boolean movingPiece = animatedPiece != null && last != null
                            && row == last.toRow && col == last.toCol;
                    boolean draggedOrigin = dragging && row == pressedRow && col == pressedCol;
                    if (piece != null && !movingPiece && !draggedOrigin) {
                        drawPiece(g, piece, x + squareSize / 2, y + squareSize / 2, squareSize);
                    }
                    if (row == 7) drawCoordinate(g, (char) ('a' + col) + "", x, boardY + boardSize);
                    if (col == 0) drawRank(g, "" + (8 - row), boardX - GUTTER, y);
                }
            }
                    if (currentTurnInCheck) {
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < 8; col++) {
                        ChessGame.Piece piece = game.pieceAt(row, col);
                        if (piece != null && piece.type == ChessGame.Type.KING && piece.color == game.currentTurn()) {
                            g.setColor(new Color(195, 71, 61, 120));
                            g.fillRect(boardX + col * squareSize, boardY + row * squareSize, squareSize, squareSize);
                        }
                    }
                }
            }
            if (animatedPiece != null && animationFrom != null && animationTo != null) {
                double progress = Math.min(1.0, (System.nanoTime() - animationStarted) / (ANIMATION_MS * 1_000_000.0));
                double eased = progress * progress * (3 - 2 * progress);
                int x = (int) (animationFrom.x + (animationTo.x - animationFrom.x) * eased);
                int y = (int) (animationFrom.y + (animationTo.y - animationFrom.y) * eased);
                drawPiece(g, animatedPiece, x, y, squareSize);
            }
            if (dragging && draggedPiece != null && dragPoint != null) {
                drawPiece(g, draggedPiece, dragPoint.x, dragPoint.y, squareSize);
            }
            g.dispose();
        }

        private void drawMoveHint(Graphics2D g, int row, int col) {
            int x = boardX + col * squareSize;
            int y = boardY + row * squareSize;
            ChessGame.Piece target = game.pieceAt(row, col);
            if (target == null) {
                g.setColor(new Color(24, 39, 34, 88));
                int diameter = squareSize / 4;
                g.fillOval(x + (squareSize - diameter) / 2, y + (squareSize - diameter) / 2, diameter, diameter);
            } else {
                g.setColor(new Color(230, 203, 123, 210));
                g.setStroke(new BasicStroke(Math.max(3, squareSize / 19f)));
                g.drawOval(x + 4, y + 4, squareSize - 8, squareSize - 8);
            }
        }

        private void drawPiece(Graphics2D g, ChessGame.Piece piece, int centerX, int centerY, int cell) {
            String glyph = pieceSymbol(piece);
            int fontSize = (int) (cell * 0.76);
            if (pieceFont == null || pieceFontSize != fontSize) {
                pieceFont = new Font("Serif", Font.PLAIN, fontSize);
                pieceFontSize = fontSize;
            }
            g.setFont(pieceFont);
            FontMetrics metrics = g.getFontMetrics();
            int x = centerX - metrics.stringWidth(glyph) / 2;
            int y = centerY + (metrics.getAscent() - metrics.getDescent()) / 2;
            g.setColor(new Color(0, 0, 0, 70));
            g.drawString(glyph, x + 1, y + 2);
            g.setColor(piece.color == ChessGame.Color.WHITE ? new Color(255, 252, 239) : new Color(29, 37, 35));
            g.drawString(glyph, x, y);
        }

        private void drawCoordinate(Graphics2D g, String text, int x, int y) {
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(MUTED);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(text, x + (squareSize - metrics.stringWidth(text)) / 2, y + GUTTER - 7);
        }

        private void drawRank(Graphics2D g, String text, int x, int y) {
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(MUTED);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(text, x + GUTTER - metrics.stringWidth(text) - 6,
                    y + (squareSize + metrics.getAscent() - metrics.getDescent()) / 2);
        }
    }

    private boolean isLegalDestination(int row, int col) {
        for (ChessGame.Move move : selectedMoves) {
            if (move.toRow == row && move.toCol == col) return true;
        }
        return false;
    }

    private String pieceSymbol(ChessGame.Piece piece) {
        switch (piece.type) {
            case KING: return piece.color == ChessGame.Color.WHITE ? "\u2654" : "\u265A";
            case QUEEN: return piece.color == ChessGame.Color.WHITE ? "\u2655" : "\u265B";
            case ROOK: return piece.color == ChessGame.Color.WHITE ? "\u2656" : "\u265C";
            case BISHOP: return piece.color == ChessGame.Color.WHITE ? "\u2657" : "\u265D";
            case KNIGHT: return piece.color == ChessGame.Color.WHITE ? "\u2658" : "\u265E";
            case PAWN: return piece.color == ChessGame.Color.WHITE ? "\u2659" : "\u265F";
            default: return "";
        }
    }
}
