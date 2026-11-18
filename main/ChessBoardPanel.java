import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicComboBoxUI;

/** A themed chess UI that presents and controls the separate ChessGame rules engine. */
public final class ChessBoardPanel extends JPanel {
    private static final String UI_FONT_FAMILY = "Segoe UI";
    private static final Font FONT_OVERLINE = new Font(UI_FONT_FAMILY, Font.BOLD, 10);
    private static final Font FONT_LABEL = new Font(UI_FONT_FAMILY, Font.BOLD, 11);
    private static final Font FONT_FIELD = new Font(UI_FONT_FAMILY, Font.PLAIN, 12);
    private static final Font FONT_BODY = new Font(UI_FONT_FAMILY, Font.PLAIN, 13);
    private static final Font FONT_BRAND = new Font("Segoe UI Black", Font.BOLD, 22);
    private static final Font FONT_BUTTON = new Font(UI_FONT_FAMILY, Font.BOLD, 13);
    private static final Font FONT_STATUS = new Font(UI_FONT_FAMILY, Font.BOLD, 16);
    private static final Font FONT_COORDINATE = new Font(UI_FONT_FAMILY, Font.BOLD, 10);
    private static final Color BACKGROUND = new Color(19, 27, 28);
    private static final Color SURFACE = new Color(31, 41, 41);
    private static final Color SURFACE_ACTIVE = new Color(41, 54, 51);
    private static final Color TEXT = new Color(242, 239, 226);
    private static final Color MUTED = new Color(156, 167, 157);
    private static final Color ACCENT = new Color(214, 177, 104);
    private static final Color SQUARE_LIGHT = new Color(231, 222, 196);
    private static final Color SQUARE_DARK = new Color(77, 112, 99);
    private static final Color MOVE_MARK = new Color(203, 183, 107, 165);
    private static final Color BOARD_SHADOW = new Color(0, 0, 0, 70);
    private static final Color SELECTION_OVERLAY = new Color(221, 177, 86, 135);
    private static final Color CHECK_OVERLAY = new Color(195, 71, 61, 120);
    private static final Color MOVE_DOT = new Color(24, 39, 34, 88);
    private static final Color CAPTURE_RING = new Color(230, 203, 123, 210);
    private static final Color PIECE_SHADOW = new Color(0, 0, 0, 70);
    private static final Color WHITE_PIECE = new Color(255, 252, 239);
    private static final Color BLACK_PIECE = new Color(29, 37, 35);
    private static final int ANIMATION_MS = 210;
    private static final String DEFAULT_WEB_URL = "https://aivan-bash.github.io/simple_chess_java/";

    private final ChessGame game = new ChessGame();
    private final BoardCanvas board = new BoardCanvas();
    private final JPanel contentHost = new JPanel(new BorderLayout());
    private final JPanel content = new JPanel();
    private final JPanel boardFrame = new JPanel(new BorderLayout());
    private JPanel sidebar;
    private JScrollPane responsiveScrollPane;
    private boolean stackedLayout;
    private final JLabel whiteCard = new JLabel();
    private final JLabel blackCard = new JLabel();
    private final JLabel status = new JLabel("White to move");
    private final JLabel instructions = new JLabel();
    private final JButton motionButton = new ThemedButton("Motion: On");
    private final JButton webButton = new ThemedButton("Open web version");
    private final JComboBox<String> opponentChoice = new ThemedComboBox(new String[]{"Two players", "Play the computer"});
    private final JComboBox<String> colorChoice = new ThemedComboBox(new String[]{"I play White", "I play Black"});
    private final JComboBox<String> difficultyChoice = new ThemedComboBox(new String[]{"Casual", "Thoughtful"});
    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<ChessGame.Move> selectedMoves = new ArrayList<>();
    private final boolean[][] legalDestinations = new boolean[8][8];
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
        JLabel brand = new JLabel("SIMPLE");
        brand.setFont(FONT_BRAND);
        brand.setForeground(TEXT);
        JLabel chessWord = new JLabel("CHESS");
        chessWord.setFont(FONT_BRAND);
        chessWord.setForeground(ACCENT);
        JPanel wordmark = new JPanel();
        wordmark.setLayout(new BoxLayout(wordmark, BoxLayout.X_AXIS));
        wordmark.setOpaque(false);
        wordmark.add(brand);
        wordmark.add(Box.createHorizontalStrut(7));
        wordmark.add(chessWord);
        JLabel tagline = new JLabel("A quiet match, one move at a time");
        tagline.setFont(FONT_BODY);
        tagline.setForeground(MUTED);
        JPanel title = new JPanel();
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.setOpaque(false);
        title.add(wordmark);
        title.add(Box.createVerticalStrut(3));
        title.add(tagline);
        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.X_AXIS));
        identity.setOpaque(false);
        identity.add(new BrandMark());
        identity.add(Box.createHorizontalStrut(13));
        identity.add(title);
        header.add(identity, BorderLayout.WEST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 2, 18, 0));

        content.setLayout(new BorderLayout(24, 0));
        content.setOpaque(false);
        boardFrame.setOpaque(false);
        boardFrame.add(board, BorderLayout.CENTER);
        content.add(boardFrame, BorderLayout.CENTER);
        sidebar = buildSidebar();
        content.add(sidebar, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        contentHost.setOpaque(false);
        contentHost.add(content, BorderLayout.CENTER);
        add(contentHost, BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) { updateResponsiveLayout(); }
        });
        refresh();
    }

    private void updateResponsiveLayout() {
        boolean useStackedLayout = getWidth() < 920 || getHeight() < 650;
        resizeBoardForCurrentWidth(useStackedLayout);
        if (useStackedLayout == stackedLayout) return;

        stackedLayout = useStackedLayout;
        content.removeAll();
        if (stackedLayout) {
            content.setLayout(new BorderLayout(0, 16));
            content.add(boardFrame, BorderLayout.CENTER);
            content.add(sidebar, BorderLayout.SOUTH);
            if (responsiveScrollPane == null) {
                responsiveScrollPane = new JScrollPane();
                responsiveScrollPane.setBorder(BorderFactory.createEmptyBorder());
                responsiveScrollPane.setOpaque(false);
                responsiveScrollPane.getViewport().setOpaque(false);
                responsiveScrollPane.getViewport().setBackground(BACKGROUND);
                responsiveScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                responsiveScrollPane.getVerticalScrollBar().setUnitIncrement(24);
            }
            responsiveScrollPane.setViewportView(content);
            contentHost.removeAll();
            contentHost.add(responsiveScrollPane, BorderLayout.CENTER);
        } else {
            content.setLayout(new BorderLayout(24, 0));
            content.add(boardFrame, BorderLayout.CENTER);
            content.add(sidebar, BorderLayout.EAST);
            responsiveScrollPane.setViewportView(null);
            contentHost.removeAll();
            contentHost.add(content, BorderLayout.CENTER);
        }
        contentHost.revalidate();
        contentHost.repaint();
    }

    private void resizeBoardForCurrentWidth(boolean useStackedLayout) {
        int preferredExtent = 660;
        if (useStackedLayout) {
            int availableWidth = getWidth() - getInsets().left - getInsets().right - 32;
            int availableHeight = getHeight() - 220;
            preferredExtent = Math.max(320, Math.min(660, Math.min(availableWidth, availableHeight)));
        }
        Dimension currentSize = board.getPreferredSize();
        if (currentSize.width == preferredExtent && currentSize.height == preferredExtent) return;
        board.setPreferredSize(new Dimension(preferredExtent, preferredExtent));
        board.revalidate();
    }

    /** Creates the matching image used by the desktop window and taskbar. */
    public static BufferedImage createAppIcon() {
        int size = 64;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BACKGROUND);
        g.fillRoundRect(1, 1, size - 2, size - 2, 16, 16);
        g.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 190));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(2, 2, size - 5, size - 5, 15, 15);
        g.setFont(new Font("Serif", Font.PLAIN, 48));
        FontMetrics metrics = g.getFontMetrics();
        String knight = "\u2658";
        int x = (size - metrics.stringWidth(knight)) / 2;
        int y = (size - metrics.getHeight()) / 2 + metrics.getAscent() - 2;
        g.setColor(BOARD_SHADOW);
        g.drawString(knight, x + 1, y + 2);
        g.setColor(TEXT);
        g.drawString(knight, x, y);
        g.dispose();
        return image;
    }

    private static final class BrandMark extends JPanel {
        BrandMark() {
            setOpaque(false);
            setPreferredSize(new Dimension(48, 48));
            setMinimumSize(new Dimension(48, 48));
            setMaximumSize(new Dimension(48, 48));
            getAccessibleContext().setAccessibleName("Simple Chess icon");
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(SURFACE_ACTIVE);
            g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 13, 13);
            g.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 150));
            g.setStroke(new BasicStroke(1.3f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 13, 13);
            g.setFont(new Font("Serif", Font.PLAIN, 38));
            FontMetrics metrics = g.getFontMetrics();
            String knight = "\u2658";
            int x = (getWidth() - metrics.stringWidth(knight)) / 2;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent() - 2;
            g.setColor(PIECE_SHADOW);
            g.drawString(knight, x + 1, y + 1);
            g.setColor(TEXT);
            g.drawString(knight, x, y);
            g.dispose();
        }
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(246, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        JLabel gameLabel = new JLabel("THE MATCH");
        gameLabel.setFont(FONT_LABEL);
        gameLabel.setForeground(MUTED);
        gameLabel.setAlignmentX(LEFT_ALIGNMENT);
        side.add(Box.createVerticalStrut(5));
        side.add(gameLabel);
        side.add(Box.createVerticalStrut(13));
        JButton newGame = new ThemedButton("New game");
        styleButton(newGame, true);
        newGame.setAlignmentX(LEFT_ALIGNMENT);
        newGame.setToolTipText("Start a fresh chess game");
        newGame.addActionListener(event -> startNewGame());
        side.add(newGame);
        side.add(Box.createVerticalStrut(14));
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
        statusLabel.setFont(FONT_OVERLINE);
        statusLabel.setForeground(MUTED);
        status.setFont(FONT_STATUS);
        status.setForeground(TEXT);
        statusBox.add(statusLabel);
        statusBox.add(Box.createVerticalStrut(7));
        statusBox.add(status);
        side.add(statusBox);
        side.add(Box.createVerticalStrut(22));

        JLabel howTo = new JLabel("YOUR TURN");
        howTo.setFont(FONT_LABEL);
        howTo.setForeground(ACCENT);
        howTo.setAlignmentX(LEFT_ALIGNMENT);
        side.add(howTo);
        side.add(Box.createVerticalStrut(9));
        instructions.setText("<html><div style='width:205px; line-height:1.55'>"
                + "The board rotates to face the player whose turn it is. Select a piece and choose a highlighted square. To castle, select your king, then click the rook."
                + "</div></html>");
        instructions.setFont(FONT_BODY);
        instructions.setForeground(MUTED);
        instructions.setAlignmentX(LEFT_ALIGNMENT);
        side.add(instructions);

        side.add(Box.createVerticalGlue());
        webButton.setText("Open web version   \u2197");
        styleButton(webButton, false);
        webButton.setToolTipText("Open Simple Chess in your browser");
        webButton.setAlignmentX(LEFT_ALIGNMENT);
        webButton.addActionListener(event -> openWebVersion());
        side.add(webButton);
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

    private void openWebVersion() {
        String url = System.getProperty("simpleChess.webUrl");
        if (url == null || url.trim().isEmpty()) url = System.getenv("SIMPLE_CHESS_WEB_URL");
        if (url == null || url.trim().isEmpty()) url = DEFAULT_WEB_URL;

        try {
            URI address = URI.create(url.trim());
            String scheme = address.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("The web URL must begin with http:// or https://.");
            }
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new UnsupportedOperationException("This computer does not provide a browser launcher.");
            }
            Desktop.getDesktop().browse(address);
        } catch (Exception error) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not open the web version. Check your browser and web URL setting.\n\n" + error.getMessage(),
                    "Web version", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addChoice(JPanel parent, String title, JComboBox<String> choice) {
        JLabel label = new JLabel(title);
        label.setFont(FONT_OVERLINE);
        label.setForeground(MUTED);
        label.setAlignmentX(LEFT_ALIGNMENT);
        choice.setFont(FONT_FIELD);
        choice.setForeground(TEXT);
        choice.setBackground(SURFACE);
        choice.setPreferredSize(new Dimension(220, 42));
        choice.setMinimumSize(new Dimension(170, 42));
        choice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
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

    private boolean isBoardFlipped() {
        return isBotGame()
                ? botColor() == ChessGame.Color.WHITE
                : game.currentTurn() == ChessGame.Color.BLACK;
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
        card.setText("<html><div style='padding:2px 0;font-family:Segoe UI,Arial,sans-serif'><span style='font-size:27px;color:#d6b168'>"
                + icon + "</span>&nbsp;&nbsp;<b style='font-family:Segoe UI,Arial,sans-serif;font-size:12px;letter-spacing:1px'>" + name
                + "</b><br><span style='color:#9ca79d;font-size:11px'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                + player + "</span></div></html>");
        card.setFont(FONT_FIELD);
        card.setForeground(TEXT);
        card.setOpaque(true);
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE, 1), BorderFactory.createEmptyBorder(7, 13, 7, 10)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
    }

    private void styleButton(JButton button, boolean primary) {
        button.setFont(FONT_BUTTON);
        button.setForeground(primary ? BACKGROUND : TEXT);
        button.setBackground(primary ? ACCENT : SURFACE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        button.setRolloverEnabled(true);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, primary ? 48 : 42));
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

    /** Rounded selector with a matching popup and a short focus highlight animation. */
    private static final class ThemedComboBox extends JComboBox<String> {
        private float focusAmount;
        private float focusTarget;
        private float popupAmount;
        private float popupTarget;
        private boolean hovered;
        private Timer focusAnimation;
        private Timer popupAnimation;

        ThemedComboBox(String[] values) {
            super(values);
            setFont(FONT_BODY);
            setForeground(TEXT);
            setBackground(SURFACE);
            setOpaque(false);
            setFocusable(true);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 36));
            setUI(new BasicComboBoxUI() {
                @Override protected JButton createArrowButton() {
                    JButton arrow = new JButton();
                    arrow.setOpaque(false);
                    arrow.setContentAreaFilled(false);
                    arrow.setBorder(BorderFactory.createEmptyBorder());
                    arrow.setFocusable(false);
                    arrow.setPreferredSize(new Dimension(30, 30));
                    return arrow;
                }
            });
            setRenderer((list, value, index, selected, focused) -> {
                JLabel item = new JLabel(value == null ? "" : value);
                item.setOpaque(true);
                item.setFont(FONT_FIELD);
                item.setBorder(BorderFactory.createEmptyBorder(index < 0 ? 0 : 8, 12,
                        index < 0 ? 0 : 8, 12));
                item.setForeground(index < 0 ? TEXT : selected ? TEXT : MUTED);
                item.setBackground(index < 0 ? SURFACE : selected ? SURFACE_ACTIVE : SURFACE);
                return item;
            });
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent event) { hovered = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent event) { hovered = false; repaint(); }
            });
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent event) { animateFocus(1f); }
                @Override public void focusLost(java.awt.event.FocusEvent event) { animateFocus(0f); }
            });
            addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent event) { animatePopup(1f); }
                @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent event) { animatePopup(0f); }
                @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent event) { animatePopup(0f); }
            });
        }

        private void animateFocus(float target) {
            focusTarget = target;
            if (focusAnimation != null && focusAnimation.isRunning()) focusAnimation.stop();
            focusAnimation = new Timer(15, event -> {
                float delta = focusTarget - focusAmount;
                if (Math.abs(delta) < .06f) {
                    focusAmount = focusTarget;
                    focusAnimation.stop();
                } else focusAmount += delta * .38f;
                repaint();
            });
            focusAnimation.start();
        }

        private void animatePopup(float target) {
            popupTarget = target;
            if (popupAnimation != null && popupAnimation.isRunning()) popupAnimation.stop();
            popupAnimation = new Timer(15, event -> {
                float delta = popupTarget - popupAmount;
                if (Math.abs(delta) < .06f) {
                    popupAmount = popupTarget;
                    popupAnimation.stop();
                } else popupAmount += delta * .38f;
                repaint();
            });
            popupAnimation.start();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(hovered || popupAmount > 0 ? SURFACE_ACTIVE : SURFACE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            Color edge = new Color(
                    (int) (64 + (ACCENT.getRed() - 64) * focusAmount),
                    (int) (80 + (ACCENT.getGreen() - 80) * focusAmount),
                    (int) (74 + (ACCENT.getBlue() - 74) * focusAmount));
            g.setColor(edge);
            g.setStroke(new BasicStroke(1f + focusAmount));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
            if (focusAmount > 0) {
                g.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), (int) (24 * focusAmount)));
                g.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 9, 9);
            }
            g.setFont(getFont());
            g.setColor(isEnabled() ? TEXT : MUTED);
            FontMetrics metrics = g.getFontMetrics();
            String value = String.valueOf(getSelectedItem());
            int baseline = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            int maxTextWidth = Math.max(0, getWidth() - 54);
            if (metrics.stringWidth(value) > maxTextWidth) {
                while (!value.isEmpty() && metrics.stringWidth(value + "…") > maxTextWidth) {
                    value = value.substring(0, value.length() - 1);
                }
                value += "…";
            }
            g.drawString(value, 13, baseline);

            int arrowX = getWidth() - 21, arrowY = getHeight() / 2;
            g.setColor(ACCENT);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Graphics2D chevron = (Graphics2D) g.create();
            chevron.rotate(Math.PI * popupAmount, arrowX, arrowY);
            chevron.drawLine(arrowX - 4, arrowY - 2, arrowX, arrowY + 2);
            chevron.drawLine(arrowX, arrowY + 2, arrowX + 4, arrowY - 2);
            chevron.dispose();
            g.dispose();
        }
    }

    private void clickSquare(int row, int col) {
        if (animationTimer != null || !isHumanTurn()) return;
        if (selectedRow >= 0) {
            ChessGame.Piece selectedPiece = game.pieceAt(selectedRow, selectedCol);
            ChessGame.Piece clickedPiece = game.pieceAt(row, col);
            if (selectedPiece != null && selectedPiece.type == ChessGame.Type.KING
                    && clickedPiece != null && clickedPiece.type == ChessGame.Type.ROOK
                    && clickedPiece.color == selectedPiece.color && row == selectedRow) {
                for (ChessGame.Move move : selectedMoves) {
                    int rookCol = move.toCol > selectedCol ? 7 : 0;
                    if (Math.abs(move.toCol - selectedCol) == 2 && col == rookCol) {
                        attemptMove(selectedRow, selectedCol, move.toRow, move.toCol, true);
                        return;
                    }
                }
            }
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
            selectPiece(row, col);
            updateSelectionHint();
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
        for (boolean[] row : legalDestinations) Arrays.fill(row, false);
        instructions.setText("<html><div style='width:205px; line-height:1.55'>"
                + "The board rotates to face the player whose turn it is. Select a piece and choose a highlighted square. To castle, select your king, then click the rook."
                + "</div></html>");
    }

    private void updateSelectionHint() {
        ChessGame.Piece piece = game.pieceAt(selectedRow, selectedCol);
        String hint = "Select a highlighted destination to move.";
        if (piece != null && piece.type == ChessGame.Type.KING
                && selectedMoves.stream().anyMatch(move -> Math.abs(move.toCol - selectedCol) == 2)) {
            hint = "Castle by clicking your rook, or choose the king's highlighted square.";
        } else if (piece != null && piece.type == ChessGame.Type.PAWN
                && selectedMoves.stream().anyMatch(move -> move.toCol != selectedCol
                    && game.pieceAt(move.toRow, move.toCol) == null)) {
            hint = "En passant is available: capture the adjacent pawn on the highlighted square.";
        }
        instructions.setText("<html><div style='width:205px; line-height:1.55'>" + hint + "</div></html>");
    }

    private void selectPiece(int row, int col) {
        selectedRow = row;
        selectedCol = col;
        selectedMoves = game.legalMovesFrom(row, col);
        for (boolean[] destinations : legalDestinations) Arrays.fill(destinations, false);
        for (ChessGame.Move move : selectedMoves) legalDestinations[move.toRow][move.toCol] = true;
    }

    private void refresh() {
        ChessGame.Color turn = game.currentTurn();
        currentTurnInCheck = game.isInCheck(turn);
        boolean hasLegalMoves = game.hasLegalMoves();
        boolean terminal = !hasLegalMoves;
        whiteCard.setText(playerCardText("\u2654", "WHITE", isBotGame()
                ? (botColor() == ChessGame.Color.WHITE ? "Computer" : "You") : "Player one", ChessGame.Color.WHITE));
        blackCard.setText(playerCardText("\u265A", "BLACK", isBotGame()
                ? (botColor() == ChessGame.Color.BLACK ? "Computer" : "You") : "Player two", ChessGame.Color.BLACK));
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

    private String playerCardText(String icon, String name, String player, ChessGame.Color color) {
        StringBuilder capturedPieces = new StringBuilder();
        for (ChessGame.Type type : game.capturedBy(color)) {
            capturedPieces.append(pieceSymbol(type, color.opposite()));
        }
        if (capturedPieces.length() == 0) capturedPieces.append("\u2014");

        int advantage = materialValue(color) - materialValue(color.opposite());
        String score = advantage > 0
                ? "<b style='color:#d6b168;font-size:11px'>+" + advantage + "</b>"
                : "";
        return "<html><table width='205' cellpadding='0' cellspacing='0'><tr>"
                + "<td rowspan='2' width='34'><span style='font-size:27px;color:#d6b168'>" + icon + "</span></td>"
                + "<td><b style='font-size:12px;letter-spacing:1px'>" + name + "</b></td>"
                + "<td rowspan='2' align='right'><span style='font-size:17px'>" + capturedPieces + "</span><br/>"
                + score + "</td></tr><tr><td><span style='color:#9ca79d;font-size:11px'>" + player
                + "</span></td></tr></table></html>";
    }

    private int materialValue(ChessGame.Color color) {
        int value = 0;
        for (ChessGame.Type type : game.capturedBy(color)) {
            switch (type) {
                case QUEEN: value += 9; break;
                case ROOK: value += 5; break;
                case BISHOP:
                case KNIGHT: value += 3; break;
                case PAWN: value += 1; break;
                default: break;
            }
        }
        return value;
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
            selectPiece(pressedRow, pressedCol);
            updateSelectionHint();
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));
        }

        private int[] squareAt(Point point) {
            updateBoardGeometry();
            if (squareSize <= 0 || point.x < boardX || point.y < boardY
                    || point.x >= boardX + boardSize || point.y >= boardY + boardSize) return null;
            int displayRow = (point.y - boardY) / squareSize;
            int displayCol = (point.x - boardX) / squareSize;
            return isBoardFlipped()
                    ? new int[]{7 - displayRow, 7 - displayCol}
                    : new int[]{displayRow, displayCol};
        }

        Point squareCenter(int row, int col) {
            updateBoardGeometry();
            int displayRow = isBoardFlipped() ? 7 - row : row;
            int displayCol = isBoardFlipped() ? 7 - col : col;
            return new Point(boardX + displayCol * squareSize + squareSize / 2,
                    boardY + displayRow * squareSize + squareSize / 2);
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
            g.setColor(BOARD_SHADOW);
            g.fillRoundRect(boardX - 5, boardY - 5, boardSize + 10, boardSize + 10, 12, 12);

            ChessGame.Move last = game.lastMove();
            boolean flipped = isBoardFlipped();
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    int displayRow = flipped ? 7 - row : row;
                    int displayCol = flipped ? 7 - col : col;
                    int x = boardX + displayCol * squareSize;
                    int y = boardY + displayRow * squareSize;
                    boolean light = (row + col) % 2 == 0;
                    g.setColor(light ? SQUARE_LIGHT : SQUARE_DARK);
                    g.fillRect(x, y, squareSize, squareSize);
                    if (last != null && ((row == last.fromRow && col == last.fromCol)
                            || (row == last.toRow && col == last.toCol))) {
                        g.setColor(MOVE_MARK);
                        g.fillRect(x, y, squareSize, squareSize);
                    }
                    if (row == selectedRow && col == selectedCol) {
                        g.setColor(SELECTION_OVERLAY);
                        g.fillRect(x, y, squareSize, squareSize);
                    }
                    if (legalDestinations[row][col]) drawMoveHint(g, row, col);
                    ChessGame.Piece piece = game.pieceAt(row, col);
                    boolean movingPiece = animatedPiece != null && last != null
                            && row == last.toRow && col == last.toCol;
                    boolean draggedOrigin = dragging && row == pressedRow && col == pressedCol;
                    if (piece != null && !movingPiece && !draggedOrigin) {
                        drawPiece(g, piece, x + squareSize / 2, y + squareSize / 2, squareSize);
                    }
                    if (displayRow == 7) drawCoordinate(g, (char) ('a' + col) + "", x, boardY + boardSize);
                    if (displayCol == 0) drawRank(g, "" + (8 - row), boardX - GUTTER, y);
                }
            }
            if (currentTurnInCheck) {
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < 8; col++) {
                        ChessGame.Piece piece = game.pieceAt(row, col);
                        if (piece != null && piece.type == ChessGame.Type.KING && piece.color == game.currentTurn()) {
                            int displayRow = flipped ? 7 - row : row;
                            int displayCol = flipped ? 7 - col : col;
                            g.setColor(CHECK_OVERLAY);
                            g.fillRect(boardX + displayCol * squareSize, boardY + displayRow * squareSize,
                                    squareSize, squareSize);
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
            int displayRow = isBoardFlipped() ? 7 - row : row;
            int displayCol = isBoardFlipped() ? 7 - col : col;
            int x = boardX + displayCol * squareSize;
            int y = boardY + displayRow * squareSize;
            ChessGame.Piece target = game.pieceAt(row, col);
            ChessGame.Piece selectedPiece = selectedRow < 0 ? null : game.pieceAt(selectedRow, selectedCol);
            boolean enPassant = selectedPiece != null && selectedPiece.type == ChessGame.Type.PAWN
                    && selectedCol != col && target == null;
            boolean castle = selectedPiece != null && selectedPiece.type == ChessGame.Type.KING
                    && Math.abs(selectedCol - col) == 2;
            if (target == null && !enPassant && !castle) {
                g.setColor(MOVE_DOT);
                int diameter = squareSize / 4;
                g.fillOval(x + (squareSize - diameter) / 2, y + (squareSize - diameter) / 2, diameter, diameter);
            } else {
                g.setColor(CAPTURE_RING);
                g.setStroke(new BasicStroke(Math.max(3, squareSize / 19f)));
                if (castle) {
                    int diameter = squareSize / 3;
                    g.drawOval(x + (squareSize - diameter) / 2, y + (squareSize - diameter) / 2,
                            diameter, diameter);
                } else {
                    g.drawOval(x + 4, y + 4, squareSize - 8, squareSize - 8);
                }
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
            g.setColor(PIECE_SHADOW);
            g.drawString(glyph, x + 1, y + 2);
            g.setColor(piece.color == ChessGame.Color.WHITE ? WHITE_PIECE : BLACK_PIECE);
            g.drawString(glyph, x, y);
        }

        private void drawCoordinate(Graphics2D g, String text, int x, int y) {
            g.setFont(FONT_COORDINATE);
            g.setColor(MUTED);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(text, x + (squareSize - metrics.stringWidth(text)) / 2, y + GUTTER - 7);
        }

        private void drawRank(Graphics2D g, String text, int x, int y) {
            g.setFont(FONT_COORDINATE);
            g.setColor(MUTED);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(text, x + GUTTER - metrics.stringWidth(text) - 6,
                    y + (squareSize + metrics.getAscent() - metrics.getDescent()) / 2);
        }
    }

    private String pieceSymbol(ChessGame.Piece piece) {
        return pieceSymbol(piece.type, piece.color);
    }

    private String pieceSymbol(ChessGame.Type type, ChessGame.Color color) {
        switch (type) {
            case KING: return color == ChessGame.Color.WHITE ? "\u2654" : "\u265A";
            case QUEEN: return color == ChessGame.Color.WHITE ? "\u2655" : "\u265B";
            case ROOK: return color == ChessGame.Color.WHITE ? "\u2656" : "\u265C";
            case BISHOP: return color == ChessGame.Color.WHITE ? "\u2657" : "\u265D";
            case KNIGHT: return color == ChessGame.Color.WHITE ? "\u2658" : "\u265E";
            case PAWN: return color == ChessGame.Color.WHITE ? "\u2659" : "\u265F";
            default: return "";
        }
    }
}
