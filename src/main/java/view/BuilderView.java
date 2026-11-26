package view;

import interface_adapter.build_circuit.BuildCircuitController;
import interface_adapter.build_circuit.BuildCircuitState;
import interface_adapter.build_circuit.BuildCircuitViewModel;
import interface_adapter.save_built_circuit.SaveBuiltCircuitController;
import interface_adapter.save_built_circuit.SaveBuiltCircuitViewModel;
import interface_adapter.import_circuit.ImportCircuitViewModel;
import interface_adapter.use_built_circuit.UseBuiltCircuitController;


import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.Locale;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Objects;

/** Displays circuit build requests, explanations, truth tables, and errors. */
public final class BuilderView extends JPanel {
    private static final long serialVersionUID = 1L;

    private final transient BuildCircuitController controller;

    private final JTextArea description = new JTextArea(4, 60);
    private final JButton buildButton = new JButton("Build");
    private final JLabel busy = Styles.mutedLabel("");
    private final JLabel circuitName = new JLabel(" ");
    private final JLabel error = Styles.errorLabel();
    private final JTextArea explanation = new JTextArea(6, 40);
    private final JTextArea truthTable = new JTextArea(12, 40);
    private final CircuitCanvas canvas = new CircuitCanvas();
    private final transient SaveBuiltCircuitController saveController;
    private final JButton saveButton = new JButton("Save as .circ");
    private final JLabel saveMessage = Styles.mutedLabel("");
    private boolean hasBuiltCircuit;
    private final transient UseBuiltCircuitController useController;
    private final JButton useButton = new JButton("Use as current circuit");

    /**
     * Creates the Builder screen.
     *
     * @param controller the controller that submits build requests
     * @param viewModel the source of build results
     */
    public BuilderView(
            BuildCircuitController controller,
            BuildCircuitViewModel viewModel,
            SaveBuiltCircuitController saveController,
            SaveBuiltCircuitViewModel saveViewModel,
            UseBuiltCircuitController useController,
            ImportCircuitViewModel importViewModel) {
        super(new BorderLayout(Styles.GAP, Styles.GAP));
        this.controller = Objects.requireNonNull(controller, "controller");
        this.saveController = Objects.requireNonNull(saveController, "saveController");
        this.useController = Objects.requireNonNull(useController, "useController");
        Objects.requireNonNull(importViewModel, "importViewModel");
        Objects.requireNonNull(saveViewModel, "saveViewModel");
        Objects.requireNonNull(viewModel, "viewModel");

        setBorder(Styles.padding());
        add(requestPanel(), BorderLayout.NORTH);
        add(resultPanel(), BorderLayout.CENTER);

        buildButton.addActionListener(event -> build());
        saveButton.addActionListener(event -> save());

        useButton.addActionListener(event -> useBuiltCircuit());

        importViewModel.addPropertyChangeListener(OnEventThread.listener(event -> {
            String message = importViewModel.getState().getError();
            if (!message.isEmpty()) {
                Styles.setMessage(error, message);
            }
        }));

        saveViewModel.addPropertyChangeListener(OnEventThread.listener(event -> {
            var state = saveViewModel.getState();
            Styles.setMessage(saveMessage, state.message());
            Styles.setMessage(error, state.error());
        }));
        viewModel.addPropertyChangeListener(OnEventThread.listener(
                event -> display((BuildCircuitState) event.getNewValue())));
        display(viewModel.getState());
    }

    private JPanel requestPanel() {
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        JLabel label = new JLabel("Describe your circuit (1–500 characters)");
        label.setLabelFor(description);

        JPanel inputPanel = new JPanel(new BorderLayout(Styles.GAP, Styles.GAP));
        inputPanel.add(label, BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(description), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.GAP, 0));
        controls.add(buildButton);
        controls.add(saveButton);
        controls.add(useButton);
        controls.add(busy);
        controls.add(saveMessage);
        inputPanel.add(controls, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(Styles.GAP, Styles.GAP));
        panel.add(Styles.mutedLabel("For practice and learning"), BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(error, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel resultPanel() {
        explanation.setEditable(false);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);

        truthTable.setEditable(false);
        truthTable.setFont(Styles.MONOSPACED);

        canvas.setEmptyMessage("Build a circuit to see its preview here.");

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Circuit", canvas);
        tabs.addTab("Explanation", new JScrollPane(explanation));
        tabs.addTab("Truth table", new JScrollPane(truthTable));

        JPanel panel = new JPanel(new BorderLayout(Styles.GAP, Styles.GAP));
        panel.add(circuitName, BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private void build() {
        if (!buildButton.isEnabled()) {
            return;
        }

        String request = description.getText();
        Styles.setMessage(error, "");
        setBusy(true);
        Styles.setMessage(saveMessage, "");

        BackgroundTask.run(
                () -> controller.build(request),
                () -> setBusy(false),
                message -> Styles.setMessage(error, message));
    }

    private void display(BuildCircuitState state) {
        Styles.setMessage(circuitName, state.circuitName());
        Styles.setMessage(error, state.error());
        explanation.setText(state.explanation());
        explanation.setCaretPosition(0);
        truthTable.setText(state.truthTableText());
        truthTable.setCaretPosition(0);
        canvas.setCircuit(state.components(), state.wires());
        hasBuiltCircuit = !state.circuitName().isBlank();
        saveButton.setEnabled(hasBuiltCircuit && buildButton.isEnabled());
        useButton.setEnabled(hasBuiltCircuit && buildButton.isEnabled());
    }

    private void setBusy(boolean working) {
        buildButton.setEnabled(!working);
        description.setEnabled(!working);
        saveButton.setEnabled(!working && hasBuiltCircuit);
        useButton.setEnabled(!working && hasBuiltCircuit);
        Styles.setMessage(busy, working ? "Working..." : "");
    }

    private void useBuiltCircuit() {
        if (!useButton.isEnabled()) {
            return;
        }

        Styles.setMessage(error, "");
        Styles.setMessage(saveMessage, "");
        setBusy(true);

        BackgroundTask.run(
                () -> useController.useBuiltCircuit(),
                () -> setBusy(false),
                message -> Styles.setMessage(error, message));
    }

    private void save() {
        if (!saveButton.isEnabled()) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save built circuit");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Logisim circuits (*.circ)", "circ"));
        chooser.setSelectedFile(new File("built-circuit.circ"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".circ")) {
            target = new File(target.getParentFile(), target.getName() + ".circ");
        }

        if (target.exists()) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Replace " + target.getName() + "?",
                    "Confirm replacement",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        String path = target.getAbsolutePath();
        Styles.setMessage(error, "");
        Styles.setMessage(saveMessage, "");
        setBusy(true);

        BackgroundTask.run(
                () -> saveController.save(path),
                () -> setBusy(false),
                message -> Styles.setMessage(error, message));
    }
}
