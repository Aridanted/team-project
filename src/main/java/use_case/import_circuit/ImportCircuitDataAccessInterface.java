package use_case.import_circuit;

import entity.circuit.Circuit;

/** Gateway for storing whichever circuit the user is currently working with. */
public interface ImportCircuitDataAccessInterface {

    /**
     * @param circuit the circuit that was just opened successfully
     */
    void setCurrentCircuit(Circuit circuit);
}
