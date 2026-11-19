package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import interface_adapter.tutor_chat.ChatBubble;
import interface_adapter.tutor_chat.TutorChatController;
import interface_adapter.tutor_chat.TutorChatState;
import interface_adapter.tutor_chat.TutorChatViewModel;

/**
 * The right side of the workspace: chat with the AI tutor about the circuit open on the left.
 * Sending waits for the AI in the background; the student's message shows at once and comes back
 * to the text box if the tutor could not answer.
 */
public final class TutorChatView extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int MIN_WIDTH = 300;
    private static final float TITLE_FONT_SIZE = 14f;
    private static final int HINT_LEVEL_1 = 1;
    private static final int HINT_LEVEL_2 = 2;
    private static final int HINT_LEVEL_3 = 3;

    static final String INTRO = "Ask the tutor about your circuit. Open and check it on the left "
            + "first so the tutor can see which rows fail.";

    private final transient TutorChatController controller;

    private final ChatBubbleList transcript = new ChatBubbleList();
    private final JScrollPane transcriptScroll = new JScrollPane(transcript,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton clearButton = new JButton("Clear");
    private final JButton hint1Button = new JButton("Hint 1");
    private final JButton hint2Button = new JButton("Hint 2");
    private final JButton hint3Button = new JButton("Hint 3");
    private final JLabel busy = Styles.mutedLabel(" ");
    private final JLabel error = Styles.errorLabel();

    private transient List<ChatBubble> messages = List.of();
    private String pending;

    /**
     * Constructs a TutorChatView connected to the given controller and view model.
     *
     * @param controller the tutor chat controller
     * @param viewModel the tutor chat view model
     */
    public TutorChatView(TutorChatController controller, TutorChatViewModel viewModel) {
        super(new BorderLayout(Styles.GAP, Styles.GAP));
        this.controller = Objects.requireNonNull(controller, "controller");
        setBorder(Styles.padding());
        setMinimumSize(new Dimension(MIN_WIDTH, 0));

        add(header(), BorderLayout.NORTH);
        add(transcriptScroll, BorderLayout.CENTER);
        add(inputArea(), BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendWithHint(0));
        input.addActionListener(e -> sendWithHint(0));
        clearButton.addActionListener(e -> controller.clear());
        hint1Button.addActionListener(e -> sendWithHint(HINT_LEVEL_1));
        hint2Button.addActionListener(e -> sendWithHint(HINT_LEVEL_2));
        hint3Button.addActionListener(e -> sendWithHint(HINT_LEVEL_3));

        viewModel.addPropertyChangeListener(OnEventThread.listener(
                e -> show((TutorChatState) e.getNewValue())));
        render();
    }

    /** @return the conversation as plain text, or empty when nobody has said anything yet */
    Optional<DownloadBar.Content> transcriptDownload() {
        return messages.isEmpty() ? Optional.empty() : Optional.of(DownloadBar.text(transcriptText(messages)));
    }

    /** @return each message as "Sender: text", separated by blank lines */
    static String transcriptText(List<ChatBubble> messages) {
        final StringBuilder text = new StringBuilder();
        for (ChatBubble message : messages) {
            text.append(message.sender()).append(": ").append(message.text()).append("\n\n");
        }
        return text.toString();
    }

    private JPanel header() {
        final JPanel row = new JPanel(new BorderLayout(Styles.GAP, 0));
        final JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.GAP, 0));
        final JLabel title = new JLabel("AI tutor");
        title.setFont(title.getFont().deriveFont(Font.BOLD, TITLE_FONT_SIZE));
        left.add(title);
        left.add(busy);
        row.add(left, BorderLayout.CENTER);
        row.add(clearButton, BorderLayout.EAST);
        return row;
    }

    private JPanel inputArea() {
        input.setToolTipText("Ask about your circuit, then press Enter");

        final JPanel hintRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.GAP, 0));
        hintRow.add(new JLabel("Hints:"));
        hintRow.add(hint1Button);
        hintRow.add(hint2Button);
        hintRow.add(hint3Button);

        final JPanel sendRow = new JPanel(new BorderLayout(Styles.GAP, 0));
        sendRow.add(input, BorderLayout.CENTER);
        sendRow.add(sendButton, BorderLayout.EAST);

        final JPanel controls = new JPanel(new BorderLayout(0, Styles.GAP));
        controls.add(hintRow, BorderLayout.NORTH);
        controls.add(sendRow, BorderLayout.SOUTH);

        final JPanel area = new JPanel(new BorderLayout());
        area.add(controls, BorderLayout.NORTH);
        area.add(error, BorderLayout.SOUTH);
        return area;
    }

    private void sendWithHint(int hintLevel) {
        final String typed = input.getText().strip();
        final String text = typed.isEmpty() && hintLevel > 0
                ? "Please give me hint level " + hintLevel + " for my circuit."
                : typed;
        if (text.isEmpty() || pending != null) {
            return;
        }
        pending = text;
        input.setText("");
        Styles.setMessage(error, "");
        setBusy(true);
        render();
        BackgroundTask.run(() -> controller.send(text, hintLevel), () -> {
            restorePending();
            setBusy(false);
        }, message -> Styles.setMessage(error, message));
    }

    private void show(TutorChatState state) {
        messages = state.getMessages();
        Styles.setMessage(error, state.getError());
        if (!state.getError().isEmpty()) {
            restorePending();
        }
        pending = null;
        render();
    }

    /** Puts an unanswered message back in the text box so the student can resend it. */
    private void restorePending() {
        if (pending != null && input.getText().isEmpty()) {
            input.setText(pending);
        }
        pending = null;
        render();
    }

    private void setBusy(boolean isBusy) {
        sendButton.setEnabled(!isBusy);
        clearButton.setEnabled(!isBusy);
        hint1Button.setEnabled(!isBusy);
        hint2Button.setEnabled(!isBusy);
        hint3Button.setEnabled(!isBusy);
        Styles.setMessage(busy, isBusy ? "Tutor is thinking..." : "");
    }

    private void render() {
        final List<ChatBubble> shown = new ArrayList<>(messages);
        if (pending != null) {
            shown.add(new ChatBubble(true, "You", pending));
        }
        transcript.setMessages(shown, INTRO);
        SwingUtilities.invokeLater(() -> {
            final JScrollBar bar = transcriptScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }
}
