package view;

import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.util.List;

/**
 * The Workspace tab: the circuit with its expected truth table on the left and the AI tutor on
 * the right, so the student can look at the circuit while they chat, with a bar underneath to
 * export what is on screen. It only arranges the views; each one keeps its own controllers.
 */
public final class WorkspaceView extends JPanel {

    private static final long serialVersionUID = 1L;

    static final double LEFT_SHARE = 0.62;
    static final double DRAWING_SHARE = 0.55;

    /**
     * @param circuit the drawing, its warnings and "Open circuit..."
     * @param table   the expected truth table, its Load / Save / Check buttons and the results
     * @param tutor   the chat with the AI tutor
     */
    public WorkspaceView(CircuitView circuit, TruthTableView table, TutorChatView tutor) {
        super(new BorderLayout());

        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT, circuit, table);
        left.setResizeWeight(DRAWING_SHARE);
        left.setOneTouchExpandable(true);
        left.setContinuousLayout(true);
        left.setBorder(null);
        Layouts.placeDividerOnFirstShow(left, DRAWING_SHARE);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, tutor);
        split.setResizeWeight(LEFT_SHARE);
        split.setContinuousLayout(true);
        split.setBorder(null);
        Layouts.placeDividerOnFirstShow(split, LEFT_SHARE);
        add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(new JSeparator(), BorderLayout.NORTH);
        bottom.add(new DownloadBar(List.of(
                new DownloadBar.Item("Results (.csv)", "results.csv", table::resultsDownload,
                        "Check the circuit first, then download the results."),
                new DownloadBar.Item("Chat (.txt)", "tutor-chat.txt", tutor::transcriptDownload,
                        "Chat with the tutor first."),
                new DownloadBar.Item("Circuit image (.png)", "circuit.png", circuit::imageDownload,
                        "Open a circuit first."))), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }
}
