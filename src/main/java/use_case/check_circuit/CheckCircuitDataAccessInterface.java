package use_case.check_circuit;

import entity.circuit.Circuit;
import entity.truthtable.CheckResult;

import java.util.Optional;

/** What the check use case needs from the session: the open circuit, and a place for the result. */
public interface CheckCircuitDataAccessInterface {

    Optional<Circuit> getCurrentCircuit();

    /** Remembers the latest result so other features (such as the tutor) can use it. */
    void setLastCheck(CheckResult result);
}
