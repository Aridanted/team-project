package use_case.import_circuit;

/** Output port for presenting the result of opening a circuit file. */
public interface ImportCircuitOutputBoundary {

    /**
     * @param outputData the opened circuit, formatted for the view
     */
    void prepareSuccessView(ImportCircuitOutputData outputData);

    /**
     * @param errorMessage explanation shown to the user; the previous circuit stays current
     */
    void prepareFailView(String errorMessage);
}
