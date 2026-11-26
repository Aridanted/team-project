package interface_adapter.save_built_circuit;

import use_case.save_built_circuit.SaveBuiltCircuitInputBoundary;

import java.util.Objects;

/** Forwards requests to save the latest built circuit. */
public final class SaveBuiltCircuitController {
    private final SaveBuiltCircuitInputBoundary interactor;

    /**
     * Creates the save controller.
     *
     * @param interactor the use case that saves and verifies the circuit
     */
    public SaveBuiltCircuitController(SaveBuiltCircuitInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor, "interactor");
    }

    /**
     * Requests saving and verification at the chosen destination.
     *
     * @param path the destination Logisim file path
     */
    public void save(String path) {
        interactor.execute(path);
    }
}
