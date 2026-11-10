package entity.truthtable;

import entity.simulation.Logic;

import java.util.List;

/**
 * An immutable truth table. Every row has one value per input label and one per output label,
 * in label order.
 */
public record TruthTable(List<String> inputLabels, List<String> outputLabels, List<Row> rows) {

    /**
     * One row of a truth table. In an expected (spec) table an output of {@link Logic#UNKNOWN}
     * means "don't care".
     */
    public record Row(List<Logic> inputs, List<Logic> outputs) {

        public Row {
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
        }
    }

    public TruthTable {
        inputLabels = List.copyOf(inputLabels);
        outputLabels = List.copyOf(outputLabels);
        rows = List.copyOf(rows);
        for (Row row : rows) {
            if (row.inputs().size() != inputLabels.size() || row.outputs().size() != outputLabels.size()) {
                throw new IllegalArgumentException("row " + row + " does not match the table's columns");
            }
        }
    }
}
