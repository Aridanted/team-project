package entity.simulation;

import entity.circuit.Circuit;

import java.util.Map;

/** Computes a circuit's outputs for one assignment of its inputs. */
public interface CircuitEvaluator {

    /**
     * @param circuit       the circuit to evaluate
     * @param inputsByLabel a value for every input pin label (exact labels as in the circuit);
     *                      missing inputs are treated as {@link Logic#UNKNOWN}
     * @return the value of every output pin, keyed by its label
     * @throws NotSimulatableException if the circuit has unsupported components or a feedback loop
     */
    Map<String, Logic> evaluate(Circuit circuit, Map<String, Logic> inputsByLabel)
            throws NotSimulatableException;
}
