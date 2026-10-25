package entity.truthtable;

import entity.simulation.Logic;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The outcome of checking a circuit against an expected truth table.
 *
 * @param expected the expected table that was checked
 * @param rows     one result per expected row, in the table's order
 */
public record CheckResult(TruthTable expected, List<RowResult> rows) {

    /**
     * The outcome of checking one expected row against the circuit.
     *
     * @param index          0-based position of the row in the expected table
     * @param expected       expected outputs ({@link Logic#UNKNOWN} = don't care)
     * @param actual         outputs the circuit produced
     * @param failingOutputs labels of outputs whose actual value differs from a concrete expected one
     */
    public record RowResult(int index, List<Logic> inputs, List<Logic> expected, List<Logic> actual,
                            List<String> failingOutputs) {

        public RowResult {
            inputs = List.copyOf(inputs);
            expected = List.copyOf(expected);
            actual = List.copyOf(actual);
            failingOutputs = List.copyOf(failingOutputs);
        }

        public boolean passed() {
            return failingOutputs.isEmpty();
        }
    }

    public CheckResult {
        rows = List.copyOf(rows);
    }

    public int passedRows() {
        return (int) rows.stream().filter(RowResult::passed).count();
    }

    public int totalRows() {
        return rows.size();
    }

    public boolean allPassed() {
        return passedRows() == totalRows();
    }

    public List<RowResult> failingRows() {
        return rows.stream().filter(row -> !row.passed()).toList();
    }

    /** @return labels of outputs that fail in at least one row, in the table's output order */
    public List<String> failingOutputs() {
        Set<String> failing = new LinkedHashSet<>();
        rows.forEach(row -> failing.addAll(row.failingOutputs()));
        return expected.outputLabels().stream().filter(failing::contains).toList();
    }
}
