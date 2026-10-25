package entity.truthtable;

import entity.circuit.Circuit;
import entity.simulation.CircuitEvaluator;
import entity.simulation.NotSimulatableException;

import java.util.Objects;

/** Builds a circuit's complete truth table by evaluating every input combination. */
public final class TruthTableGenerator {

    /** Largest number of inputs enumerated (1024 rows). */
    public static final int MAX_INPUTS = 10;

    private final CircuitEvaluator evaluator;

    public TruthTableGenerator(CircuitEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    public TruthTable generate(Circuit circuit) throws NotSimulatableException {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
