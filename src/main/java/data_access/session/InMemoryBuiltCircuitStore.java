package data_access.session;

import entity.circuit.Circuit;
import use_case.build_circuit.BuiltCircuitDataAccessInterface;

import java.util.Objects;
import java.util.Optional;

/** Retains the latest successfully built circuit in memory. */
public final class InMemoryBuiltCircuitStore implements BuiltCircuitDataAccessInterface {
    private volatile Circuit builtCircuit;

    @Override
    public void setBuiltCircuit(Circuit circuit) {
        builtCircuit = Objects.requireNonNull(circuit, "circuit");
    }

    @Override
    public Optional<Circuit> getBuiltCircuit() {
        return Optional.ofNullable(builtCircuit);
    }
}
