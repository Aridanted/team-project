package view;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.awt.Dimension;

/** The application window: one tab per feature (Circuit, Tutor, Builder, Settings). */
public final class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    static final int WIDTH = 1100;
    static final int HEIGHT = 720;

    private final JTabbedPane tabs = new JTabbedPane();

    public MainFrame() {
        super("LogiCoach");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(tabs);
        setSize(WIDTH, HEIGHT);
        setMinimumSize(new Dimension(800, 560));
        setLocationRelativeTo(null);
    }

    public void addTab(String title, JComponent content) {
        tabs.addTab(title, content);
    }
}
