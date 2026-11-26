package view;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.awt.Dimension;

/** The application window: tabs for the Workspace (circuit and tutor), the Builder and Settings. */
public final class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    static final int WIDTH = 1240;
    static final int HEIGHT = 780;

    private final JTabbedPane tabs = new JTabbedPane();

    public MainFrame() {
        super("LogiCoach");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(tabs);
        setSize(WIDTH, HEIGHT);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    public void addTab(String title, JComponent content) {
        tabs.addTab(title, content);
    }

    /**
     * Selects an existing application tab.
     *
     * @param title the title of the tab to display
     * @throws IllegalArgumentException if no tab has that title
     */
    public void showTab(String title) {
        int index = tabs.indexOfTab(title);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown tab: " + title);
        }
        tabs.setSelectedIndex(index);
    }
}
