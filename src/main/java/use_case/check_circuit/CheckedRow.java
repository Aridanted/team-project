package use_case.check_circuit;

import java.util.List;

/**
 * One checked row, as text: values are "0", "1" or "X".
 *
 * @param failingOutputs labels of outputs that did not match; empty if the row passed
 */
public record CheckedRow(List<String> inputs, List<String> expected, List<String> actual,
                         List<String> failingOutputs) {

    public CheckedRow {
        inputs = List.copyOf(inputs);
        expected = List.copyOf(expected);
        actual = List.copyOf(actual);
        failingOutputs = List.copyOf(failingOutputs);
    }

    public boolean passed() {
        return failingOutputs.isEmpty();
    }
}
