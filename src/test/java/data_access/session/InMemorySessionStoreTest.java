package data_access.session;

import entity.circuit.Circuit;
import entity.circuit.CircuitTestFactory;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySessionStoreTest {

    @Test
    void startsEmpty() {
        InMemorySessionStore store = new InMemorySessionStore();

        assertEquals(Optional.empty(), store.currentCircuit());
    }

    @Test
    void storesCircuitAfterSet() {
        InMemorySessionStore store = new InMemorySessionStore();
        Circuit circuit = CircuitTestFactory.halfAdder();

        store.setCurrentCircuit(circuit);

        assertTrue(store.currentCircuit().isPresent());
        assertSame(circuit, store.currentCircuit().get());
    }

    @Test
    void replacesPreviousCircuit() {
        InMemorySessionStore store = new InMemorySessionStore();
        Circuit first = CircuitTestFactory.halfAdder();
        Circuit second = CircuitTestFactory.fullAdder();

        store.setCurrentCircuit(first);
        store.setCurrentCircuit(second);

        assertSame(second, store.currentCircuit().get());
    }
}
