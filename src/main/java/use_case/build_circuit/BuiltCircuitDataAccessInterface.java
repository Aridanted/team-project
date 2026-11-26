package use_case.build_circuit;

import entity.circuit.Circuit;

import java.util.Optional;

/** Stores the latest successfully built circuit for later actions. */
public interface BuiltCircuitDataAccessInterface {

    /**
     * Stores a successfully built circuit.
     *
     * @param circuit the circuit to retain
     */
    void setBuiltCircuit(Circuit circuit);

    /**
     * Returns the latest successfully built circuit.
     *
     * @return the retained circuit, or empty if none has been built
     */
    Optional<Circuit> getBuiltCircuit();
}
