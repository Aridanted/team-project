package view;

import interface_adapter.build_circuit.BuildCircuitController;
import interface_adapter.build_circuit.BuildCircuitState;
import interface_adapter.build_circuit.BuildCircuitViewModel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
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

    /**
     * Creates the Builder screen.
     *
     * @param controller the controller that submits build requests
     * @param viewModel the source of build results
     */
    public BuilderView(BuildCircuitController controller, BuildCircuitViewModel viewModel) {
        super(new BorderLayout(Styles.GAP, Styles.GAP));
        this.controller = Objects.requireNonNull(controller, "controller");
        Objects.requireNonNull(viewModel, "viewModel");

        setBorder(Styles.padding());
        add(requestPanel(), BorderLayout.NORTH);
        add(resultPanel(), BorderLayout.CENTER);
        add(error, BorderLayout.SOUTH);

        buildButton.addActionListener(event -> build());
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
        controls.add(busy);
        inputPanel.add(controls, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(Styles.GAP, Styles.GAP));
        panel.add(Styles.mutedLabel("For practice and learning"), BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
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
    }

    private void setBusy(boolean building) {
        buildButton.setEnabled(!building);
        description.setEnabled(!building);
        Styles.setMessage(busy, building ? "Building circuit..." : "");
    }
}
