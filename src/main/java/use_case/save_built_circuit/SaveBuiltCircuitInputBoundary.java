package use_case.save_built_circuit;

/** Accepts requests to save the latest successfully built circuit. */
public interface SaveBuiltCircuitInputBoundary {

    /**
     * Saves the latest built circuit to the requested destination.
     *
     * @param path the destination Logisim file path
     */
    void execute(String path);
}
