package use_case.build_circuit;

/** Accepts requests to build a circuit. */
public interface BuildCircuitInputBoundary {

    /**
     * Runs the build use case for the supplied description.
     *
     * @param inputData the student's request
     */
    void execute(BuildCircuitInputData inputData);
}
