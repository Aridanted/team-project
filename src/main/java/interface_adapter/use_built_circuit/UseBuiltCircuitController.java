package interface_adapter.use_built_circuit;

import use_case.use_built_circuit.UseBuiltCircuitInputBoundary;

import java.util.Objects;

/** Forwards requests to use the latest built circuit in Workspace. */
public final class UseBuiltCircuitController {
    private final UseBuiltCircuitInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the use case that updates the current circuit
     */
    public UseBuiltCircuitController(UseBuiltCircuitInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor, "interactor");
    }

    /** Makes the latest successfully built circuit the current circuit. */
    public void useBuiltCircuit() {
        interactor.execute();
    }
}
