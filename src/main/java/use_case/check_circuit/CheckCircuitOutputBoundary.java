package use_case.check_circuit;

/** Receives the outcome of a check. */
public interface CheckCircuitOutputBoundary {

    void prepareSuccessView(CheckCircuitOutputData outputData);

    /** @param errorMessage why no check could be made, ready to show to the user */
    void prepareFailView(String errorMessage);
}
