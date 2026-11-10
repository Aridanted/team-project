package interface_adapter.import_circuit;

import use_case.import_circuit.ImportCircuitInputBoundary;
import use_case.import_circuit.ImportCircuitInputData;

import java.util.Objects;

/** Handles the user's request to open a circuit file. */
public final class ImportCircuitController {

    private final ImportCircuitInputBoundary interactor;

    /**
     * @param interactor import-circuit use case entry point
     */
    public ImportCircuitController(ImportCircuitInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor, "interactor");
    }

    /**
     * @param filePath path of the .circ file to open
     */
    public void execute(String filePath) {
        interactor.execute(new ImportCircuitInputData(filePath));
    }
}
