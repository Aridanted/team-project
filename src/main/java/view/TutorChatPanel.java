package view;

import interface_adapter.TutorController;
import interface_adapter.TutorViewModel;

import javax.swing.*;
import java.awt.*;

public class TutorChatPanel extends JPanel {
    private final JTextArea chatArea = new JTextArea(15, 30);
    private final JTextField inputField = new JTextField(20);
    private final JButton sendButton = new JButton("Sent");

    // --- BỔ SUNG CHO ISSUE #7: 3 nút Hint 1 / 2 / 3 ---
    private final JButton hint1Button = new JButton("Hint 1");
    private final JButton hint2Button = new JButton("Hint 2");
    private final JButton hint3Button = new JButton("Hint 3");

    public TutorChatPanel(TutorController controller, TutorViewModel viewModel) {
        setLayout(new BorderLayout());

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hintPanel.add(new JLabel("Hints:"));
        hintPanel.add(hint1Button);
        hintPanel.add(hint2Button);
        hintPanel.add(hint3Button);
        bottomPanel.add(hintPanel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(inputField);
        inputPanel.add(sendButton);
        bottomPanel.add(inputPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                chatArea.append("Student: " + text + "\n");
                controller.sendMessage(text);
                inputField.setText("");
            }
        });

        hint1Button.addActionListener(e -> triggerHint(controller, 1));
        hint2Button.addActionListener(e -> triggerHint(controller, 2));
        hint3Button.addActionListener(e -> triggerHint(controller, 3));

        viewModel.addPropertyChangeListener(evt -> {
            if ("response".equals(evt.getPropertyName())) {
                chatArea.append((String) evt.getNewValue() + "\n\n");
            }
        });
    }

    private void triggerHint(TutorController controller, int hintLevel) {
        String currentInput = inputField.getText().trim();
        String promptText = currentInput.isEmpty()
                ? "Please give me a hint for my circuit."
                : currentInput;

        chatArea.append("Student [Hint " + hintLevel + "]: " + promptText + "\n");
        controller.sendMessage(promptText, null, hintLevel);
        if (!currentInput.isEmpty()) {
            inputField.setText("");
        }
    }
}