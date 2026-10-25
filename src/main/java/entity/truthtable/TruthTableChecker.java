package entity.truthtable;

import entity.circuit.Circuit;
import entity.simulation.CircuitEvaluator;
import entity.simulation.NotSimulatableException;

import java.util.Objects;

/** Compares a circuit with an expected truth table, row by row. */
public final class TruthTableChecker {

    private final CircuitEvaluator evaluator;

    public TruthTableChecker(CircuitEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    /**
     * @throws LabelMismatchException  if the expected table's labels differ from the circuit's pins
     * @throws NotSimulatableException if the circuit cannot be simulated
     */
    public CheckResult check(Circuit circuit, TruthTable expected)
            throws LabelMismatchException, NotSimulatableException {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
