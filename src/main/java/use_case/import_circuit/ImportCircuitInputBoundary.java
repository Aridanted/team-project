package use_case.import_circuit;

/** Input port for opening a Logisim circuit file and making it the current circuit. */
public interface ImportCircuitInputBoundary {

    /**
     * @param inputData path of the .circ file to open
     */
    void execute(ImportCircuitInputData inputData);
}
