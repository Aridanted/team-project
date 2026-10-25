package data_access.session;

import entity.circuit.Circuit;
import use_case.import_circuit.ImportCircuitSessionInterface;

import java.util.Objects;
import java.util.Optional;

/** In-memory holder for the circuit the user is currently editing or simulating. */
public final class InMemorySessionStore implements ImportCircuitSessionInterface {

    private Circuit currentCircuit;

    @Override
    public synchronized void setCurrentCircuit(Circuit circuit) {
        this.currentCircuit = Objects.requireNonNull(circuit, "circuit");
    }

    /**
     * @return the circuit last opened successfully, if any
     */
    public synchronized Optional<Circuit> currentCircuit() {
        return Optional.ofNullable(currentCircuit);
    }
}
