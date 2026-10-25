package interface_adapter.check_circuit;

import java.util.List;

/**
 * One row of the results table, ready to display.
 *
 * @param result "PASS", or "FAIL" followed by the failing outputs, for example "FAIL (Sum)"
 */
public record ResultRow(List<String> inputs, List<String> expected, List<String> actual, boolean passed,
                        String result) {

    public ResultRow {
        inputs = List.copyOf(inputs);
        expected = List.copyOf(expected);
        actual = List.copyOf(actual);
    }
}
