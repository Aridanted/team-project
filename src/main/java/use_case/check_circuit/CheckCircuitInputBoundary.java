package use_case.check_circuit;

/** Checks the current circuit against an expected truth table. */
public interface CheckCircuitInputBoundary {

    void execute(CheckCircuitInputData inputData);
}
