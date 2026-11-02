package view;

import interface_adapter.TutorController;
import interface_adapter.TutorViewModel;

import javax.swing.*;
import java.awt.*;

public class TutorChatPanel extends JPanel {
    private final JTextArea chatArea = new JTextArea(15, 30);
    private final JTextField inputField = new JTextField(20);
    private final JButton sendButton = new JButton("Gửi");

    public TutorChatPanel(TutorController controller, TutorViewModel viewModel) {
        setLayout(new BorderLayout());

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(inputField);
        inputPanel.add(sendButton);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> {
            String text = inputField.getText();
            if (!text.isEmpty()) {
                chatArea.append("Student: " + text + "\n");
                controller.sendMessage(text);
                inputField.setText("");
            }
        });

        viewModel.addPropertyChangeListener(evt -> {
            if ("response".equals(evt.getPropertyName())) {
                chatArea.append((String) evt.getNewValue() + "\n\n");
            }
        });
    }
}
