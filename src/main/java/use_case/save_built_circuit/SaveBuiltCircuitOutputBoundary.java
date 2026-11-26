package use_case.save_built_circuit;

/** Presents the outcome of saving a built circuit. */
public interface SaveBuiltCircuitOutputBoundary {

    /**
     * Reports a successfully saved and verified circuit.
     *
     * @param path the destination file path
     */
    void prepareSuccessView(String path);

    /**
     * Reports why saving or verification failed.
     *
     * @param message the failure message
     */
    void prepareFailView(String message);
}
