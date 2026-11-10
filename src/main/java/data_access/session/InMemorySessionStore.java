package data_access.session;

import entity.circuit.Circuit;
import entity.truthtable.CheckResult;
import use_case.check_circuit.CheckCircuitDataAccessInterface;
import use_case.import_circuit.ImportCircuitDataAccessInterface;

import java.util.Objects;
import java.util.Optional;

/**
 * In-memory holder for the circuit the user has open and its latest check result. Implements
 * each use case's small session interface (interface segregation), so use cases share data
 * without calling each other.
 */
public final class InMemorySessionStore implements ImportCircuitDataAccessInterface,
        CheckCircuitDataAccessInterface {

    private Circuit currentCircuit;
    private CheckResult lastCheck;

    /** Also forgets the previous check, which belonged to the previous circuit. */
    @Override
    public synchronized void setCurrentCircuit(Circuit circuit) {
        this.currentCircuit = Objects.requireNonNull(circuit, "circuit");
        this.lastCheck = null;
    }

    /** @return the circuit last opened successfully, if any */
    @Override
    public synchronized Optional<Circuit> getCurrentCircuit() {
        return Optional.ofNullable(currentCircuit);
    }

    @Override
    public synchronized void setLastCheck(CheckResult result) {
        this.lastCheck = Objects.requireNonNull(result, "result");
    }

    /** @return the latest check of the current circuit, if any */
    public synchronized Optional<CheckResult> getLastCheck() {
        return Optional.ofNullable(lastCheck);
    }
}
