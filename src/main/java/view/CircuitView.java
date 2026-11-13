package view;

import interface_adapter.import_circuit.ImportCircuitController;
import interface_adapter.import_circuit.ImportCircuitState;
import interface_adapter.import_circuit.ImportCircuitViewModel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/** Left half of the Circuit tab: the "Open circuit..." button, the drawing and any warnings. */
public final class CircuitView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ImportCircuitViewModel viewModel;
    private final ImportCircuitController controller;

    private final JFileChooser chooser = new JFileChooser();
    private final JLabel title = new JLabel("No circuit open");
    private final JLabel error = Styles.errorLabel();
    private final CircuitCanvas canvas = new CircuitCanvas();
    private final DefaultListModel<String> warnings = new DefaultListModel<>();

    public CircuitView(ImportCircuitViewModel viewModel, ImportCircuitController controller) {
        super(new BorderLayout(0, 6));
        this.viewModel = viewModel;
        this.controller = controller;
        chooser.setFileFilter(new FileNameExtensionFilter("Logisim circuits (*.circ)", "circ"));

        JButton open = new JButton("Open circuit...");
        open.addActionListener(event -> chooseCircuit());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(open);
        toolbar.add(title);
        JPanel top = new JPanel(new BorderLayout());
        top.add(toolbar, BorderLayout.NORTH);
        top.add(error, BorderLayout.SOUTH);

        canvas.setBorder(BorderFactory.createLineBorder(Styles.MUTED));
        JScrollPane warningList = new JScrollPane(new JList<>(warnings));
        warningList.setBorder(BorderFactory.createTitledBorder("Warnings"));
        warningList.setPreferredSize(new Dimension(0, 110));

        add(top, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(warningList, BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        viewModel.addPropertyChangeListener(OnEventThread.listener(
                event -> showState(viewModel.getState())));
    }

    private void chooseCircuit() {
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            controller.execute(chooser.getSelectedFile().getPath());
        }
    }

    private void showState(ImportCircuitState state) {
        Styles.setMessage(error, state.getError());
        if (!state.isLoaded()) {
            return;
        }
        title.setText(state.getCircuitName() + "  (" + state.getSummary() + ")");
        canvas.setCircuit(state.getComponents(), state.getWires());
        warnings.clear();
        warnings.addAll(state.getWarnings());
    }
}
