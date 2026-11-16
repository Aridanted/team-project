package use_case.build_circuit;

import entity.circuit.GateType;
import entity.design.CircuitDesign;
import entity.design.GateSpec;
import entity.expression.ExpressionParser;
import entity.simulation.CircuitSimulator;
import entity.simulation.Logic;
import entity.truthtable.TruthTable;
import entity.truthtable.TruthTableGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildCircuitInteractorTest {
    private static final CircuitDesignGenerator UNEXPECTED_GENERATOR = description -> {
        throw new AssertionError("Invalid descriptions should never reach the generator");
    };

    private final RecordingPresenter presenter = new RecordingPresenter();

    @Test
    void rejectsBlankDescriptionBeforeCallingGenerator() {
        interactor(UNEXPECTED_GENERATOR).execute(new BuildCircuitInputData("   "));

        assertEquals("Enter a circuit description.", presenter.error);
        assertNull(presenter.result);
    }

    @Test
    void rejectsDescriptionLongerThan500Characters() {
        interactor(UNEXPECTED_GENERATOR).execute(new BuildCircuitInputData("x".repeat(501)));

        assertEquals("Use at most 500 characters.", presenter.error);
        assertNull(presenter.result);
    }

    @Test
    void returnsSimulatedTruthTableForValidDesign() {
        BuildCircuitInteractor interactor = interactor(description -> {
            assertEquals("Connect A to Y", description);
            return passThrough("A");
        });

        interactor.execute(new BuildCircuitInputData("  Connect A to Y  "));

        assertNull(presenter.error);
        assertNotNull(presenter.result);

        TruthTable table = presenter.result.truthTable();
        assertEquals(List.of("A"), table.inputLabels());
        assertEquals(List.of("Y"), table.outputLabels());
        assertEquals(List.of(
                new TruthTable.Row(List.of(Logic.ZERO), List.of(Logic.ZERO)),
                new TruthTable.Row(List.of(Logic.ONE), List.of(Logic.ONE))), table.rows());
    }

    @Test
    void retriesAfterAFeedbackLoopAndAcceptsTheNextDesign() {
        AtomicInteger attempts = new AtomicInteger();
        CircuitDesign loop = new CircuitDesign("Loop", List.of("A"), List.of("Y"),
                List.of(new GateSpec("G1", GateType.NOT, List.of("G1"))),
                Map.of("Y", "G1"));

        BuildCircuitInteractor interactor = interactor(description -> {
            if (attempts.incrementAndGet() == 1) {
                return new GeneratedDesign(loop, Map.of());
            }
            return passThrough("A");
        });

        interactor.execute(new BuildCircuitInputData("Connect A to Y"));

        assertEquals(2, attempts.get());
        assertNull(presenter.error);
        assertNotNull(presenter.result);
        assertEquals("Pass through", presenter.result.design().name());
    }

    @Test
    void stopsAfterThreeAttemptsWhenTheExpressionKeepsDisagreeing() {
        AtomicInteger attempts = new AtomicInteger();
        BuildCircuitInteractor interactor = interactor(description -> {
            attempts.incrementAndGet();
            return passThrough("!A");
        });

        interactor.execute(new BuildCircuitInputData("Connect A to Y"));

        assertEquals(3, attempts.get());
        assertNull(presenter.result);
        assertNotNull(presenter.error);
        assertTrue(presenter.error.contains("after 3 attempts"));
        assertTrue(presenter.error.contains("Expression for output Y"));
    }

    private BuildCircuitInteractor interactor(CircuitDesignGenerator generator) {
        return new BuildCircuitInteractor(generator, new DesignValidator(), new DesignAssembler(),
                new TruthTableGenerator(new CircuitSimulator()),
                new OutputExpressionVerifier(new ExpressionParser()), presenter);
    }

    private static GeneratedDesign passThrough(String expression) {
        CircuitDesign design = new CircuitDesign("Pass through", List.of("A"), List.of("Y"),
                List.of(), Map.of("Y", "A"));
        return new GeneratedDesign(design, Map.of("Y", expression));
    }

    private static final class RecordingPresenter implements BuildCircuitOutputBoundary {
        private BuildCircuitOutputData result;
        private String error;

        @Override
        public void prepareSuccessView(BuildCircuitOutputData outputData) {
            result = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
