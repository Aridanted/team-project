package use_case.save_built_circuit;

import entity.circuit.Circuit;

/** Writes a built circuit to a file. */
public interface CircuitFileWriterInterface {

    /**
     * Writes the circuit as a Logisim file.
     *
     * @param circuit the circuit to save
     * @param path the destination file path
     * @throws CircuitFileWriteException if the circuit cannot be written
     */
    void write(Circuit circuit, String path) throws CircuitFileWriteException;
}
