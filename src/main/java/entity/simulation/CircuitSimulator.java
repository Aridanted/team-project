package entity.simulation;

import entity.circuit.Circuit;

import java.util.Map;

/** Evaluates combinational circuits with three-valued logic. */
public final class CircuitSimulator implements CircuitEvaluator {

    @Override
    public Map<String, Logic> evaluate(Circuit circuit, Map<String, Logic> inputsByLabel)
            throws NotSimulatableException {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
