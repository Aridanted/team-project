package use_case.use_built_circuit;

import entity.circuit.Circuit;
import entity.circuit.InputPin;
import entity.circuit.Orientation;
import entity.circuit.OutputPin;
import entity.circuit.Point;
import entity.circuit.Wire;
import org.junit.jupiter.api.Test;
import use_case.build_circuit.BuiltCircuitDataAccessInterface;
import use_case.build_circuit.CircuitPreviewLayout;
import use_case.import_circuit.ImportCircuitOutputBoundary;
import use_case.import_circuit.ImportCircuitOutputData;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests transferring built circuits into Workspace without files or network calls. */
class UseBuiltCircuitInteractorTest {

    @Test
    void makesTheArrangedCircuitCurrentAndPublishesItsDrawing() {
        Fixture fixture = new Fixture();

        fixture.interactor().execute();

        assertSame(fixture.arranged, fixture.current);
        assertEquals("Built circuit", fixture.output.circuitName());
        assertEquals(List.of("A"), fixture.output.inputLabels());
        assertEquals(List.of("Y"), fixture.output.outputLabels());
        assertEquals(2, fixture.output.components().size());
        assertEquals("input", fixture.output.components().getFirst().kind());
        assertEquals(100, fixture.output.components().getFirst().x());
        assertEquals("west", fixture.output.components().getLast().facing());
        assertEquals(1, fixture.output.wires().size());
        assertEquals(100, fixture.output.wires().getFirst().x1());
        assertEquals(300, fixture.output.wires().getFirst().x2());
        assertTrue(fixture.output.simulatable());
        assertTrue(fixture.output.warnings().isEmpty());
        assertEquals("", fixture.error);
    }

    @Test
    void passesTheStoredCircuitToTheLayout() {
        Fixture fixture = new Fixture();
        fixture.layout = circuit -> {
            assertSame(fixture.built, circuit);
            return fixture.arranged;
        };

        fixture.interactor().execute();

        assertSame(fixture.arranged, fixture.current);
    }

    @Test
    void preservesTheCurrentCircuitWhenNothingHasBeenBuilt() {
        Fixture fixture = new Fixture();
        Circuit previous = fixture.current;
        fixture.built = null;
        fixture.layout = circuit -> {
            throw new AssertionError("Layout must not run without a built circuit.");
        };

        fixture.interactor().execute();

        assertSame(previous, fixture.current);
        assertNull(fixture.output);
        assertEquals(
                "Build a circuit before using it in Workspace.",
                fixture.error);
    }

    @Test
    void preservesTheCurrentCircuitWhenLayoutFails() {
        Fixture fixture = new Fixture();
        Circuit previous = fixture.current;
        fixture.layout = circuit -> {
            throw new IllegalArgumentException("No safe wire route.");
        };

        fixture.interactor().execute();

        assertSame(previous, fixture.current);
        assertNull(fixture.output);
        assertTrue(fixture.error.contains("No safe wire route."));
    }

    private static Circuit circuit(String name, boolean positioned) {
        Point input = positioned ? new Point(100, 100) : new Point(0, 0);
        Point output = positioned ? new Point(300, 100) : new Point(0, 0);

        return new Circuit(
                name,
                List.of(
                        new InputPin("a", "A", 0, input, Orientation.EAST),
                        new OutputPin("y", "Y", 0, output, Orientation.WEST)),
                positioned ? List.of(new Wire(input, output, 0)) : List.of(),
                1,
                List.of());
    }

    private static final class Fixture implements
            BuiltCircuitDataAccessInterface, ImportCircuitOutputBoundary {
        private Circuit built = circuit("Built circuit", false);
        private final Circuit arranged = circuit("Built circuit", true);
        private Circuit current = circuit("Previous circuit", true);
        private ImportCircuitOutputData output;
        private String error = "";
        private CircuitPreviewLayout layout = circuit -> arranged;

        private UseBuiltCircuitInteractor interactor() {
            return new UseBuiltCircuitInteractor(
                    this,
                    layout,
                    circuit -> current = circuit,
                    this);
        }

        @Override
        public void setBuiltCircuit(Circuit circuit) {
            built = circuit;
        }

        @Override
        public Optional<Circuit> getBuiltCircuit() {
            return Optional.ofNullable(built);
        }

        @Override
        public void prepareSuccessView(ImportCircuitOutputData data) {
            assertSame(arranged, current);
            output = data;
        }

        @Override
        public void prepareFailView(String message) {
            error = message;
        }
    }
}
