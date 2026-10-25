package entity.truthtable;

import entity.simulation.Logic;

import java.util.List;

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
