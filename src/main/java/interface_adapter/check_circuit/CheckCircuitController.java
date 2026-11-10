package interface_adapter.check_circuit;

import use_case.check_circuit.CheckCircuitInputBoundary;
import use_case.check_circuit.CheckCircuitInputData;

import java.util.Objects;

/** Called by the check screen when the student presses "Check". */
public final class CheckCircuitController {

    private final CheckCircuitInputBoundary interactor;

    public CheckCircuitController(CheckCircuitInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor, "interactor");
    }

    /** @param specText the expected truth table as typed */
    public void execute(String specText) {
        interactor.execute(new CheckCircuitInputData(specText));
    }
}
