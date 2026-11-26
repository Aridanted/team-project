package use_case.use_built_circuit;

/** Accepts requests to make the latest built circuit the current circuit. */
public interface UseBuiltCircuitInputBoundary {

    /**
     * Loads the latest successfully built circuit into the current workspace.
     */
    void execute();
}
