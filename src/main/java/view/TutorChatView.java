package view;

import interface_adapter.tutor_chat.ChatBubble;
import interface_adapter.tutor_chat.TutorChatController;
import interface_adapter.tutor_chat.TutorChatState;
import interface_adapter.tutor_chat.TutorChatViewModel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Objects;

/**
 * The Tutor tab: chat with the AI tutor about the circuit open in the Circuit tab. Sending waits
 * for the AI in the background; the student's message shows at once and comes back to the text
 * box if the tutor could not answer.
 */
public final class TutorChatView extends JPanel {

    private static final long serialVersionUID = 1L;

    static final String INTRO = "Ask the tutor about your circuit. Open and check it in the Circuit tab "
            + "first so the tutor can see which rows fail.";

    private final transient TutorChatController controller;

    private final JTextArea transcript = new JTextArea();
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

        add(header(), BorderLayout.NORTH);
        add(transcriptArea(), BorderLayout.CENTER);
        add(inputArea(), BorderLayout.SOUTH);

        sendButton.addActionListener(e -> send());
        input.addActionListener(e -> send());
        clearButton.addActionListener(e -> controller.clear());

        viewModel.addPropertyChangeListener(OnEventThread.listener(
                e -> show((TutorChatState) e.getNewValue())));
        render();
    }

    private JPanel header() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.GAP, 0));
        JLabel title = new JLabel("AI tutor");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        row.add(title);
        row.add(clearButton);
        row.add(busy);
        return row;
    }

    private JScrollPane transcriptArea() {
        transcript.setEditable(false);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);
        transcript.setBorder(Styles.padding());
        return new JScrollPane(transcript);
    }

    private JPanel inputArea() {
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
        StringBuilder text = new StringBuilder();
        if (messages.isEmpty() && pending == null) {
            text.append(INTRO);
        }
        for (ChatBubble message : messages) {
            text.append(message.sender()).append(": ").append(message.text()).append("\n\n");
        }
        if (pending != null) {
            text.append("You: ").append(pending).append("\n\n");
        }
        transcript.setText(text.toString());
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }
}
