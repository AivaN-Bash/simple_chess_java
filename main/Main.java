import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Starts the Simple Chess desktop application. */
public final class Main {
    private Main() {
        // This class only provides the application's starting point.
    }

    /** Opens the game window on Swing's event thread. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::showWindow);
    }

    /** Builds and displays the desktop game window. */
    private static void showWindow() {
        setSystemLookAndFeel();

        JFrame window = new JFrame("Simple Chess");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setIconImage(ChessBoardPanel.createAppIcon());
        window.setContentPane(new ChessBoardPanel());
        window.pack();
        window.setMinimumSize(new Dimension(680, 560));
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    /** Uses the operating system's Swing theme when available. */
    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Swing's default theme is a safe fallback if the system theme is unavailable.
        }
    }
}
