package entity.truthtable;

import java.util.List;

/**
 * An immutable truth table. Every row has one value per input label and one per output label,
 * in label order.
 */
public record TruthTable(List<String> inputLabels, List<String> outputLabels, List<TruthTableRow> rows) {

    public TruthTable {
        inputLabels = List.copyOf(inputLabels);
        outputLabels = List.copyOf(outputLabels);
        rows = List.copyOf(rows);
        for (TruthTableRow row : rows) {
            if (row.inputs().size() != inputLabels.size() || row.outputs().size() != outputLabels.size()) {
                throw new IllegalArgumentException("row " + row + " does not match the table's columns");
            }
        }
    }
}
