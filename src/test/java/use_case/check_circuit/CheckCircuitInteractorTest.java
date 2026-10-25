package use_case.check_circuit;

import entity.circuit.Circuit;
import entity.circuit.CircuitTestFactory;
import entity.simulation.CircuitSimulator;
import entity.truthtable.CheckResult;
import entity.truthtable.TruthTableChecker;
import entity.truthtable.TruthTableTextFormat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckCircuitInteractorTest {

    private static final String HALF_ADDER_SPEC = """
            inputs: A, B
            outputs: Sum, Cout
            0 0 | 0 0
            0 1 | 1 0
            1 0 | 1 0
            1 1 | 0 1
            """;

    private static final class FakeSession implements CheckCircuitDataAccessInterface {
        private final Circuit circuit;
        private CheckResult lastCheck;

        FakeSession(Circuit circuit) {
            this.circuit = circuit;
        }

        @Override
        public Optional<Circuit> getCurrentCircuit() {
            return Optional.ofNullable(circuit);
        }

        @Override
        public void setLastCheck(CheckResult result) {
            lastCheck = result;
        }
    }

    private static final class CapturingPresenter implements CheckCircuitOutputBoundary {
        private CheckCircuitOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(CheckCircuitOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }

    private final CapturingPresenter presenter = new CapturingPresenter();

    private FakeSession run(Circuit circuit, String spec) {
        FakeSession session = new FakeSession(circuit);
        new CheckCircuitInteractor(session, new TruthTableTextFormat(),
                new TruthTableChecker(new CircuitSimulator()), presenter)
                .execute(new CheckCircuitInputData(spec));
        return session;
    }

    @Test
    void correctCircuitPassesEveryRowAndIsRemembered() {
        FakeSession session = run(CircuitTestFactory.halfAdder(), HALF_ADDER_SPEC);

        assertNull(presenter.error);
        assertEquals(4, presenter.success.passedRows());
        assertEquals(4, presenter.success.totalRows());
        assertEquals(List.of("A", "B"), presenter.success.inputLabels());
        assertEquals(List.of("Sum", "Cout"), presenter.success.outputLabels());
        assertEquals(List.of(), presenter.success.failingOutputs());
        assertNotNull(session.lastCheck);
        assertTrue(session.lastCheck.allPassed());
    }

    @Test
    void reportsFailingRowsAsText() {
        run(CircuitTestFactory.halfAdder(), HALF_ADDER_SPEC.replace("1 1 | 0 1", "1 1 | 1 1"));

        assertEquals(3, presenter.success.passedRows());
        assertEquals(List.of("Sum"), presenter.success.failingOutputs());
        CheckedRow last = presenter.success.rows().get(3);
        assertEquals(List.of("1", "1"), last.inputs());
        assertEquals(List.of("1", "1"), last.expected());
        assertEquals(List.of("0", "1"), last.actual());
        assertEquals(List.of("Sum"), last.failingOutputs());
    }

    @Test
    void needsAnOpenCircuit() {
        FakeSession session = run(null, HALF_ADDER_SPEC);
        assertEquals("Open a circuit before checking it.", presenter.error);
        assertNull(session.lastCheck);
    }

    @Test
    void needsASpec() {
        run(CircuitTestFactory.halfAdder(), "  ");
        assertEquals("Enter the expected truth table first.", presenter.error);
    }

    @Test
    void reportsSpecErrorsWithLineNumbers() {
        FakeSession session = run(CircuitTestFactory.halfAdder(), "inputs: A, B\noutputs: Sum, Cout\n0 0 | 0\n");
        assertTrue(presenter.error.startsWith("line 3: "), presenter.error);
        assertNull(session.lastCheck);
    }

    @Test
    void reportsLabelMismatch() {
        run(CircuitTestFactory.halfAdder(), "inputs: A, B, Cin\noutputs: Sum, Cout\n0 0 0 | 0 0\n");
        assertEquals("Spec expects inputs {A, B, Cin} but circuit has {A, B}.", presenter.error);
    }

    @Test
    void reportsCircuitsThatCannotBeSimulated() {
        Circuit circuit = CircuitTestFactory.builder("bad")
                .input("A").unsupported("D Flip-Flop").output("Y", "A").build();
        run(circuit, "inputs: A\noutputs: Y\n0 | 0\n");
        assertEquals("The circuit contains unsupported components: D Flip-Flop.", presenter.error);
    }
}
