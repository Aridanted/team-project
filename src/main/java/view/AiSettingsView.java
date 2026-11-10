package view;

import interface_adapter.ai_settings.AiSettingsController;
import interface_adapter.ai_settings.AiSettingsState;
import interface_adapter.ai_settings.AiSettingsViewModel;
import interface_adapter.ai_settings.ProviderRow;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Settings tab: an API key per provider (masked, with a Show box and a Test button each),
 * which provider to try first, and Save.
 */
public final class AiSettingsView extends JPanel {

    private static final long serialVersionUID = 1L;

    static final String INTRO = "The tutor and the circuit builder use an AI provider. Both have free keys: Gemini at "
            + "aistudio.google.com and Groq at console.groq.com. Keys are saved only on this computer, in "
            + "data/settings.json. Without a key, the tutor still gives offline hints.";

    private final transient AiSettingsController controller;

    private final JComboBox<String> firstProvider = new JComboBox<>();
    private final JPanel keyRows = new JPanel(new GridBagLayout());
    private final JButton saveButton = new JButton("Save");
    private final JLabel status = new JLabel(" ");
    private final JLabel connection = new JLabel(" ");
    private final JLabel error = Styles.errorLabel();
    private final Map<String, JPasswordField> keyFields = new LinkedHashMap<>();
    private final List<JButton> testButtons = new ArrayList<>();
    private transient List<ProviderRow> shownProviders = List.of();

    public AiSettingsView(AiSettingsController controller, AiSettingsViewModel viewModel) {
        super(new BorderLayout());
        this.controller = Objects.requireNonNull(controller, "controller");
        setBorder(Styles.padding());

        JPanel content = new JPanel(new BorderLayout(0, Styles.GAP));
        content.add(intro(), BorderLayout.NORTH);
        content.add(form(), BorderLayout.CENTER);
        add(content, BorderLayout.NORTH);

        saveButton.addActionListener(e -> save());
        viewModel.addPropertyChangeListener(OnEventThread.listener(e -> show((AiSettingsState) e.getNewValue())));
    }

    private static JTextArea intro() {
        JTextArea text = new JTextArea(INTRO);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setOpaque(false);
        text.setFont(UIManager.getFont("Label.font"));
        text.setBorder(BorderFactory.createEmptyBorder(0, 0, Styles.GAP, 0));
        return text;
    }

    private JPanel form() {
        keyRows.setBorder(Styles.titled("API keys"));

        JPanel order = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.GAP, 0));
        order.add(new JLabel("Try first:"));
        order.add(firstProvider);
        order.add(Styles.mutedLabel("If it fails, LogiCoach tries the other provider."));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.GAP, 0));
        actions.add(saveButton);
        status.setForeground(Styles.SUCCESS);
        actions.add(status);

        JPanel messages = new JPanel(new BorderLayout());
        messages.add(connection, BorderLayout.NORTH);
        messages.add(error, BorderLayout.SOUTH);

        JPanel form = new JPanel(new BorderLayout(0, Styles.GAP));
        form.add(keyRows, BorderLayout.NORTH);
        form.add(order, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(0, Styles.GAP));
        south.add(actions, BorderLayout.NORTH);
        south.add(messages, BorderLayout.SOUTH);
        form.add(south, BorderLayout.SOUTH);
        return form;
    }

    private void show(AiSettingsState state) {
        if (!state.getProviders().equals(shownProviders)) {
            shownProviders = state.getProviders();
            showProviders(shownProviders);
        }
        status.setText(state.getStatus().isEmpty() ? " " : state.getStatus());
        connection.setForeground(state.isConnected() ? Styles.SUCCESS : Styles.ERROR);
        Styles.setMessage(connection, state.getConnectionMessage());
        Styles.setMessage(error, state.getError());
    }

    /** Rebuilds the key rows; only called when the saved settings change, so typing is never lost. */
    private void showProviders(List<ProviderRow> providers) {
        keyRows.removeAll();
        keyFields.clear();
        testButtons.clear();
        firstProvider.removeAllItems();
        for (int i = 0; i < providers.size(); i++) {
            addKeyRow(providers.get(i), i);
            firstProvider.addItem(providers.get(i).displayName());
        }
        if (!providers.isEmpty()) {
            firstProvider.setSelectedIndex(0);
        }
        keyRows.revalidate();
        keyRows.repaint();
    }

    private void addKeyRow(ProviderRow provider, int row) {
        JPasswordField field = new JPasswordField(provider.apiKey(), 36);
        char echo = field.getEchoChar();
        JCheckBox showKey = new JCheckBox("Show");
        showKey.addActionListener(e -> field.setEchoChar(showKey.isSelected() ? (char) 0 : echo));
        JButton test = new JButton("Test connection");
        test.addActionListener(e -> testConnection(provider));
        keyFields.put(provider.id(), field);
        testButtons.add(test);

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        JLabel label = new JLabel(provider.displayName() + " API key:");
        label.setLabelFor(field);
        keyRows.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        keyRows.add(field, c);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        keyRows.add(showKey, c);
        c.gridx = 3;
        keyRows.add(test, c);
    }

    private void save() {
        List<String> order = new ArrayList<>();
        int first = firstProvider.getSelectedIndex();
        if (first >= 0) {
            order.add(shownProviders.get(first).id());
        }
        shownProviders.stream().map(ProviderRow::id).filter(id -> !order.contains(id)).forEach(order::add);
        controller.save(order, typedKeys());
    }

    private Map<String, String> typedKeys() {
        Map<String, String> keys = new LinkedHashMap<>();
        keyFields.forEach((id, field) -> keys.put(id, new String(field.getPassword())));
        return keys;
    }

    private void testConnection(ProviderRow provider) {
        String key = new String(keyFields.get(provider.id()).getPassword());
        setTesting(true);
        connection.setForeground(Styles.MUTED);
        connection.setText("Testing " + provider.displayName() + "...");
        Styles.setMessage(error, "");
        BackgroundTask.run(() -> controller.testConnection(provider.id(), key), () -> setTesting(false),
                message -> Styles.setMessage(error, message));
    }

    private void setTesting(boolean testing) {
        testButtons.forEach(button -> button.setEnabled(!testing));
        saveButton.setEnabled(!testing);
    }
}
