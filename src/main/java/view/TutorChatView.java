package view;

import interface_adapter.tutor_chat.ChatBubble;
import interface_adapter.tutor_chat.TutorChatController;
import interface_adapter.tutor_chat.TutorChatState;
import interface_adapter.tutor_chat.TutorChatViewModel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The right side of the workspace: chat with the AI tutor about the circuit open on the left.
 * Sending waits for the AI in the background; the student's message shows at once and comes back
 * to the text box if the tutor could not answer.
 */
public final class TutorChatView extends JPanel {

    private static final long serialVersionUID = 1L;

    static final String INTRO = "Ask the tutor about your circuit. Open and check it on the left "
            + "first so the tutor can see which rows fail.";

    private final transient TutorChatController controller;

    private final ChatBubbleList transcript = new ChatBubbleList();
    private final JScrollPane transcriptScroll = new JScrollPane(transcript,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton clearButton = new JButton("Clear");
    private final JLabel busy = Styles.mutedLabel(" ");
    private final JLabel error = Styles.errorLabel();

    private transient List<ChatBubble> messages = List.of();
    private String pending;

    public TutorChatView(TutorChatController controller, TutorChatViewModel viewModel) {
        super(new BorderLayout(Styles.GAP, Styles.GAP));
        this.controller = Objects.requireNonNull(controller, "controller");
        setBorder(Styles.padding());
        setMinimumSize(new Dimension(300, 0));

        add(header(), BorderLayout.NORTH);
        add(transcriptScroll, BorderLayout.CENTER);
        add(inputArea(), BorderLayout.SOUTH);

        sendButton.addActionListener(e -> send());
        input.addActionListener(e -> send());
        clearButton.addActionListener(e -> controller.clear());

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
        StringBuilder text = new StringBuilder();
        for (ChatBubble message : messages) {
            text.append(message.sender()).append(": ").append(message.text()).append("\n\n");
        }
        return text.toString();
    }

    private JPanel header() {
        JPanel row = new JPanel(new BorderLayout(Styles.GAP, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.GAP, 0));
        JLabel title = new JLabel("AI tutor");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        left.add(title);
        left.add(busy);
        row.add(left, BorderLayout.CENTER);
        row.add(clearButton, BorderLayout.EAST);
        return row;
    }

    private JPanel inputArea() {
        input.setToolTipText("Ask about your circuit, then press Enter");
        JPanel row = new JPanel(new BorderLayout(Styles.GAP, 0));
        row.add(input, BorderLayout.CENTER);
        row.add(sendButton, BorderLayout.EAST);
        JPanel area = new JPanel(new BorderLayout());
        area.add(row, BorderLayout.NORTH);
        area.add(error, BorderLayout.SOUTH);
        return area;
    }

    private void send() {
        String text = input.getText().strip();
        if (text.isEmpty() || pending != null) {
            return;
        }
        pending = text;
        input.setText("");
        Styles.setMessage(error, "");
        setBusy(true);
        render();
        BackgroundTask.run(() -> controller.send(text), () -> {
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
        Styles.setMessage(busy, isBusy ? "Tutor is thinking..." : "");
    }

    private void render() {
        List<ChatBubble> shown = new ArrayList<>(messages);
        if (pending != null) {
            shown.add(new ChatBubble(true, "You", pending));
        }
        transcript.setMessages(shown, INTRO);
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = transcriptScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }
}
