package view;

import interface_adapter.check_circuit.CheckCircuitController;
import interface_adapter.check_circuit.CheckCircuitState;
import interface_adapter.check_circuit.CheckCircuitViewModel;
import interface_adapter.check_circuit.ResultRow;
import interface_adapter.check_circuit.SpecTemplate;
import interface_adapter.import_circuit.ImportCircuitController;
import interface_adapter.import_circuit.ImportCircuitState;
import interface_adapter.import_circuit.ImportCircuitViewModel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * The Circuit tab: open a .circ file, see it drawn with its summary and warnings, type or load
 * the expected truth table, and check it row by row.
 */
public final class CircuitView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient ImportCircuitController importController;
    private final transient CheckCircuitController checkController;

    private final JButton openButton = new JButton("Open .circ...");
    private final JLabel circuitName = new JLabel("No circuit open");
    private final JLabel importError = Styles.errorLabel();
    private final CircuitCanvas canvas = new CircuitCanvas();
    private final JLabel summary = Styles.mutedLabel(" ");
    private final DefaultListModel<String> warnings = new DefaultListModel<>();

    private final JTextArea specText = new JTextArea(10, 28);
    private final JButton loadSpecButton = new JButton("Load spec file...");
    private final JButton checkButton = new JButton("Check");
    private final JLabel checkSummary = new JLabel(" ");
    private final JLabel checkError = Styles.errorLabel();
    private final CheckResultsTableModel results = new CheckResultsTableModel();

    private final JFileChooser circuitChooser = new JFileChooser();
    private final JFileChooser specChooser = new JFileChooser();
    private final JLabel busy = Styles.mutedLabel(" ");
    private String lastTemplate = "";
    private boolean canCheck;

    public CircuitView(ImportCircuitController importController, ImportCircuitViewModel importViewModel,
                       CheckCircuitController checkController, CheckCircuitViewModel checkViewModel) {
        super(new BorderLayout(Styles.GAP, Styles.GAP));
        this.importController = Objects.requireNonNull(importController, "importController");
        this.checkController = Objects.requireNonNull(checkController, "checkController");
        setBorder(Styles.padding());

        circuitChooser.setFileFilter(new FileNameExtensionFilter("Logisim circuits (*.circ)", "circ"));
        specChooser.setFileFilter(new FileNameExtensionFilter("Truth tables (*.txt)", "txt"));

        add(header(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, circuitArea(), checkArea());
        split.setResizeWeight(0.55);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        openButton.addActionListener(e -> chooseCircuit());
        loadSpecButton.addActionListener(e -> chooseSpec());
        checkButton.addActionListener(e -> check());
        checkButton.setEnabled(false);

        importViewModel.addPropertyChangeListener(OnEventThread.listener(
                e -> showImport((ImportCircuitState) e.getNewValue())));
        checkViewModel.addPropertyChangeListener(OnEventThread.listener(
                e -> showCheck((CheckCircuitState) e.getNewValue())));
    }

    private JPanel header() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Styles.GAP, 0));
        circuitName.setFont(circuitName.getFont().deriveFont(Font.BOLD, 14f));
        row.add(openButton);
        row.add(circuitName);
        row.add(busy);
        JPanel header = new JPanel(new BorderLayout());
        header.add(row, BorderLayout.NORTH);
        header.add(importError, BorderLayout.SOUTH);
        return header;
    }

    private JSplitPane circuitArea() {
        canvas.setBorder(BorderFactory.createLineBorder(Styles.MUTED));
        JPanel info = new JPanel(new BorderLayout(0, Styles.GAP));
        info.setBorder(Styles.titled("Components"));
        info.add(summary, BorderLayout.NORTH);
        JPanel warningPanel = new JPanel(new BorderLayout());
        warningPanel.add(new JLabel("Warnings"), BorderLayout.NORTH);
        JList<String> warningList = new JList<>(warnings);
        warningList.setVisibleRowCount(6);
        warningPanel.add(new JScrollPane(warningList), BorderLayout.CENTER);
        info.add(warningPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, info);
        split.setResizeWeight(0.72);
        split.setBorder(null);
        return split;
    }

    private JSplitPane checkArea() {
        specText.setFont(Styles.MONOSPACED);
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(loadSpecButton);
        buttons.add(Box.createHorizontalStrut(Styles.GAP));
        buttons.add(checkButton);
        buttons.add(Box.createHorizontalGlue());

        JPanel spec = new JPanel(new BorderLayout(0, 4));
        spec.setBorder(Styles.titled("Expected truth table"));
        spec.add(new JScrollPane(specText), BorderLayout.CENTER);
        JPanel specSouth = new JPanel(new BorderLayout());
        specSouth.add(buttons, BorderLayout.NORTH);
        specSouth.add(checkError, BorderLayout.SOUTH);
        spec.add(specSouth, BorderLayout.SOUTH);

        JTable table = new JTable(results);
        table.setDefaultRenderer(Object.class, new ResultRenderer());
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        JPanel resultPanel = new JPanel(new BorderLayout(0, 4));
        resultPanel.setBorder(Styles.titled("Results"));
        resultPanel.add(checkSummary, BorderLayout.NORTH);
        resultPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, spec, resultPanel);
        split.setResizeWeight(0.4);
        split.setBorder(null);
        return split;
    }

    private void chooseCircuit() {
        if (circuitChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String path = circuitChooser.getSelectedFile().getAbsolutePath();
        setBusy(true, "Opening...");
        BackgroundTask.run(() -> importController.execute(path), () -> setBusy(false, ""),
                message -> Styles.setMessage(importError, message));
    }

    private void check() {
        String text = specText.getText();
        setBusy(true, "Checking...");
        BackgroundTask.run(() -> checkController.execute(text), () -> setBusy(false, ""),
                message -> Styles.setMessage(checkError, message));
    }

    private void chooseSpec() {
        if (specChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = specChooser.getSelectedFile();
        loadSpecButton.setEnabled(false);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }

            @Override
            protected void done() {
                loadSpecButton.setEnabled(true);
                try {
                    specText.setText(get());
                    specText.setCaretPosition(0);
                    Styles.setMessage(checkError, "");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Styles.setMessage(checkError, "Could not read " + file.getName() + ".");
                }
            }
        }.execute();
    }

    private void setBusy(boolean working, String status) {
        openButton.setEnabled(!working);
        checkButton.setEnabled(!working && canCheck);
        Styles.setMessage(busy, status);
    }

    private void showImport(ImportCircuitState state) {
        Styles.setMessage(importError, state.getError());
        if (!state.isLoaded()) {
            circuitName.setText("No circuit open");
            return;
        }
        circuitName.setText(state.getCircuitName().isBlank() ? "(unnamed circuit)" : state.getCircuitName());
        if (!state.getError().isEmpty()) {
            return;
        }
        canvas.setCircuit(state.getComponents(), state.getWires());
        Styles.setMessage(summary, state.getSummary()
                + (state.isSimulatable() ? "" : " - contains components that cannot be checked"));
        warnings.clear();
        state.getWarnings().forEach(warnings::addElement);
        if (state.getWarnings().isEmpty()) {
            warnings.addElement("No warnings.");
        }
        results.clear();
        Styles.setMessage(checkSummary, "");
        Styles.setMessage(checkError, "");
        canCheck = state.isSimulatable();
        checkButton.setEnabled(canCheck);
        if (specText.getText().isBlank() || specText.getText().equals(lastTemplate)) {
            lastTemplate = SpecTemplate.forLabels(state.getInputLabels(), state.getOutputLabels());
            specText.setText(lastTemplate);
            specText.setCaretPosition(0);
        }
    }

    private void showCheck(CheckCircuitState state) {
        Styles.setMessage(checkError, state.getError());
        if (!state.getError().isEmpty()) {
            return;
        }
        results.setResults(state.getInputLabels(), state.getOutputLabels(), state.getRows());
        checkSummary.setForeground(state.getRows().stream().allMatch(ResultRow::passed) ? Styles.SUCCESS : Styles.ERROR);
        Styles.setMessage(checkSummary, state.getSummary());
    }

    /** Colours passing rows green and failing rows red; the Result column says it in words too. */
    private final class ResultRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setHorizontalAlignment(CENTER);
            if (!selected) {
                cell.setBackground(results.passed(table.convertRowIndexToModel(row)) ? Styles.PASS_ROW : Styles.FAIL_ROW);
            }
            return cell;
        }
    }
}
