package entity.truthtable;

import entity.simulation.Logic;

import java.util.List;

/**
 * One row of a truth table. In an expected (spec) table an output of {@link Logic#UNKNOWN} means
 * "don't care".
 */
public record TruthTableRow(List<Logic> inputs, List<Logic> outputs) {

    public TruthTableRow {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }
}
