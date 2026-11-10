package use_case.check_circuit;

import java.util.List;

/**
 * @param failingOutputs outputs that fail in at least one row, in column order
 */
public record CheckCircuitOutputData(List<String> inputLabels, List<String> outputLabels,
                                     List<CheckedRow> rows, int passedRows, int totalRows,
                                     List<String> failingOutputs) {

    public CheckCircuitOutputData {
        inputLabels = List.copyOf(inputLabels);
        outputLabels = List.copyOf(outputLabels);
        rows = List.copyOf(rows);
        failingOutputs = List.copyOf(failingOutputs);
    }
}
