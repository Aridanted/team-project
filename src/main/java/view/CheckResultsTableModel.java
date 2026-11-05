package view;

import interface_adapter.check_circuit.ResultRow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * The results table: one column per input, an expected and an actual column per output, and a
 * Result column with PASS or FAIL in words (so the result never depends on colour alone).
 */
final class CheckResultsTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    static final String RESULT = "Result";

    private transient List<String> inputs = List.of();
    private transient List<String> outputs = List.of();
    private transient List<ResultRow> rows = List.of();

    void setResults(List<String> inputLabels, List<String> outputLabels, List<ResultRow> resultRows) {
        inputs = List.copyOf(inputLabels);
        outputs = List.copyOf(outputLabels);
        rows = List.copyOf(resultRows);
        fireTableStructureChanged();
    }

    void clear() {
        setResults(List.of(), List.of(), List.of());
    }

    boolean passed(int row) {
        return rows.get(row).passed();
    }

    List<String> columnNames() {
        List<String> names = new ArrayList<>(inputs);
        for (String output : outputs) {
            names.add(output + " expected");
            names.add(output + " actual");
        }
        if (!inputs.isEmpty() || !outputs.isEmpty()) {
            names.add(RESULT);
        }
        return names;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames().size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames().get(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int column) {
        ResultRow row = rows.get(rowIndex);
        if (column < inputs.size()) {
            return row.inputs().get(column);
        }
        int outputColumn = column - inputs.size();
        if (outputColumn < outputs.size() * 2) {
            int output = outputColumn / 2;
            return outputColumn % 2 == 0 ? row.expected().get(output) : row.actual().get(output);
        }
        return row.result();
    }
}
