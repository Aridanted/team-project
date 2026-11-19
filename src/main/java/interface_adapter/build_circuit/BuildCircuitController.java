package interface_adapter.build_circuit;

import use_case.build_circuit.BuildCircuitInputBoundary;
import use_case.build_circuit.BuildCircuitInputData;

import java.util.Objects;

/** Passes a student's circuit description to the build use case. */
public final class BuildCircuitController {
    private final BuildCircuitInputBoundary interactor;

    /**
     * Creates a controller for the build use case.
     *
     * @param interactor the input boundary that accepts build requests
     */
    public BuildCircuitController(BuildCircuitInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor, "interactor");
    }

    /**
     * Submits a circuit description.
     *
     * @param description the student's plain-English description
     */
    public void build(String description) {
        interactor.execute(new BuildCircuitInputData(description));
    }
}
