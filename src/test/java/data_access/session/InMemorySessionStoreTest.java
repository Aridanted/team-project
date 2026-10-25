package data_access.session;

import entity.circuit.Circuit;
import entity.circuit.CircuitTestFactory;
import entity.truthtable.CheckResult;
import entity.truthtable.TruthTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InMemorySessionStoreTest {

    private final InMemorySessionStore store = new InMemorySessionStore();

    private static CheckResult emptyCheck() {
        return new CheckResult(new TruthTable(List.of("A"), List.of("Y"), List.of()), List.of());
    }

    @Test
    void startsEmpty() {
        assertEquals(Optional.empty(), store.getCurrentCircuit());
        assertEquals(Optional.empty(), store.getLastCheck());
    }

    @Test
    void replacesPreviousCircuit() {
        Circuit first = CircuitTestFactory.halfAdder();
        Circuit second = CircuitTestFactory.fullAdder();

        store.setCurrentCircuit(first);
        store.setCurrentCircuit(second);

        assertSame(second, store.getCurrentCircuit().orElseThrow());
    }

    @Test
    void remembersTheLatestCheck() {
        store.setCurrentCircuit(CircuitTestFactory.halfAdder());
        CheckResult check = emptyCheck();

        store.setLastCheck(check);

        assertSame(check, store.getLastCheck().orElseThrow());
    }

    @Test
    void openingAnotherCircuitForgetsTheOldCheck() {
        store.setCurrentCircuit(CircuitTestFactory.halfAdder());
        store.setLastCheck(emptyCheck());

        store.setCurrentCircuit(CircuitTestFactory.fullAdder());

        assertEquals(Optional.empty(), store.getLastCheck());
    }
}
