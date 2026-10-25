package use_case.import_circuit;

import entity.circuit.Circuit;

/** Gateway the import use case needs: turns a circuit file into a {@link Circuit}. */
public interface CircuitFileDataAccessInterface {

    /**
     * @param path path of the circuit file
     * @return the circuit in the file, with any non-fatal problems in {@link Circuit#warnings()}
     * @throws CircuitParseException if the file cannot be read or is not a usable circuit
     */
    Circuit load(String path) throws CircuitParseException;
}
