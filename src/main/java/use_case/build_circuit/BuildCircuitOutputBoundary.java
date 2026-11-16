package use_case.build_circuit;

/** Receives the outcome of a build request. */
public interface BuildCircuitOutputBoundary {

    /**
     * Receives the verified circuit and truth table.
     *
     * @param outputData the successful build result
     */
    void prepareSuccessView(BuildCircuitOutputData outputData);

    /**
     * Receives a message explaining why the build failed.
     *
     * @param errorMessage the message to show the student
     */
    void prepareFailView(String errorMessage);
}
