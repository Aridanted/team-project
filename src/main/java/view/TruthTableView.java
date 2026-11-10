package view;

import interface_adapter.check_circuit.CheckCircuitController;
import interface_adapter.check_circuit.CheckCircuitState;
import interface_adapter.check_circuit.CheckCircuitViewModel;
import interface_adapter.check_circuit.ResultRow;
import interface_adapter.table_file.TableFileController;
import interface_adapter.table_file.TableFileState;
import interface_adapter.table_file.TableFileViewModel;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.io.File;

/**
 * Right half of the Circuit tab: the expected truth table editor, its Load / Save / Check
 * buttons, and the row-by-row results.
 */
public final class TruthTableView extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String EXAMPLE = """
            # Type the truth table you expect,
            # then press Check. For example:
            # inputs: A, B
            # outputs: Sum, Cout
            # 0 0 | 0 0
            # 0 1 | 1 0
            # Lines starting with # are ignored.
            # X as an output means "don't care".
            """;

    private final CheckCircuitViewModel checkViewModel;
    private final CheckCircuitController checkController;
    private final TableFileViewModel fileViewModel;
    private final TableFileController fileController;

    private final JFileChooser chooser = new JFileChooser();
    private final JTextArea editor = new JTextArea(EXAMPLE);
    private final CheckResultsTableModel resultsModel = new CheckResultsTableModel();
    private final JTable results = new JTable(resultsModel);
    private final JLabel summary = new JLabel(" ");
    private final JLabel status = Styles.errorLabel();

    public TruthTableView(CheckCircuitViewModel checkViewModel, CheckCircuitController checkController,
                          TableFileViewModel fileViewModel, TableFileController fileController) {
        super(new BorderLayout(0, 6));
        this.checkViewModel = checkViewModel;
        this.checkController = checkController;
        this.fileViewModel = fileViewModel;
        this.fileController = fileController;
        chooser.setFileFilter(new FileNameExtensionFilter("Truth tables (*.txt)", "txt"));

        editor.setFont(Styles.MONOSPACED);
        results.setDefaultRenderer(Object.class, new PassFailRenderer());
        results.setEnabled(false);
        summary.setFont(summary.getFont().deriveFont(Font.BOLD, 14f));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPanel(), resultsPanel());
        split.setResizeWeight(0.45);
        add(split, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        setBorder(Styles.padding());

        checkViewModel.addPropertyChangeListener(OnEventThread.listener(
                event -> showCheck(checkViewModel.getState())));
        fileViewModel.addPropertyChangeListener(OnEventThread.listener(this::showFileResult));
    }

    /** Forgets the shown results, for example because a different circuit was opened. */
    public void clearResults() {
        resultsModel.clear();
        Styles.setMessage(summary, " ");
    }

    /**
     * Replaces the editor text with a label template only if the student has not typed their own
     * table yet.
     */
    public void offerTemplate(String text) {
        String current = editor.getText();
        if (current.isBlank() || current.equals(EXAMPLE)) {
            editor.setText(text);
            editor.setCaretPosition(0);
        }
    }

    private JPanel editorPanel() {
        JButton load = new JButton("Load table...");
        JButton save = new JButton("Save table...");
        JButton check = new JButton("Check");
        load.addActionListener(event -> loadTable());
        save.addActionListener(event -> saveTable());
        check.addActionListener(event -> checkController.execute(editor.getText()));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(load);
        buttons.add(save);
        buttons.add(check);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(Styles.titled("Expected truth table"));
        panel.add(new JScrollPane(editor), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel resultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(Styles.titled("Results"));
        panel.add(summary, BorderLayout.NORTH);
        panel.add(new JScrollPane(results), BorderLayout.CENTER);
        return panel;
    }

    private void loadTable() {
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileController.load(chooser.getSelectedFile().getPath());
        }
    }

    private void saveTable() {
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        boolean replace = !file.exists() || JOptionPane.showConfirmDialog(this,
                file.getName() + " already exists. Replace it?", "Save table",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        if (replace) {
            fileController.save(file.getPath(), editor.getText());
        }
    }

    private void showCheck(CheckCircuitState state) {
        if (!state.getError().isEmpty()) {
            clearResults();
            showStatus(state.getError(), true);
            return;
        }
        resultsModel.setResults(state.getInputLabels(), state.getOutputLabels(), state.getRows());
        boolean allPassed = state.getRows().stream().allMatch(ResultRow::passed);
        summary.setForeground(allPassed ? Styles.SUCCESS : Styles.ERROR);
        Styles.setMessage(summary, state.getSummary());
        showStatus("", false);
    }

    private void showFileResult(PropertyChangeEvent event) {
        TableFileState state = fileViewModel.getState();
        if (TableFileViewModel.TABLE_LOADED.equals(event.getPropertyName())) {
            editor.setText(state.getTableText());
            editor.setCaretPosition(0);
        }
        boolean failed = !state.getError().isEmpty();
        showStatus(failed ? state.getError() : state.getMessage(), failed);
    }

    private void showStatus(String text, boolean isError) {
        status.setForeground(isError ? Styles.ERROR : Styles.MUTED);
        Styles.setMessage(status, text);
    }

    /** Colours passing rows green and failing rows red. */
    private final class PassFailRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, false, false, row, column);
            if (!isSelected) {
                cell.setBackground(resultsModel.passed(table.convertRowIndexToModel(row))
                        ? Styles.PASS_ROW : Styles.FAIL_ROW);
            }
            cell.setForeground(java.awt.Color.BLACK);
            setHorizontalAlignment(CENTER);
            return cell;
        }
    }
}
