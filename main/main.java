import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Starts the desktop chess app. Run with: java -cp out Main */
class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Swing's default look and feel is a suitable fallback.
            }
            JFrame window = new JFrame("Simple Chess");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(new ChessBoardPanel());
            window.pack();
            window.setMinimumSize(new java.awt.Dimension(880, 690));
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
