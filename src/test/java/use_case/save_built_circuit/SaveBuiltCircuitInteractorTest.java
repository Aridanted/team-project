package use_case.save_built_circuit;

import entity.circuit.Circuit;
import entity.circuit.Constant;
import entity.circuit.InputPin;
import entity.circuit.Orientation;
import entity.circuit.OutputPin;
import entity.circuit.Point;
import entity.simulation.CircuitSimulator;
import entity.truthtable.TruthTableGenerator;
import org.junit.jupiter.api.Test;
import use_case.build_circuit.BuiltCircuitDataAccessInterface;
import use_case.import_circuit.CircuitFileDataAccessInterface;
import use_case.import_circuit.CircuitParseException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests saving and reload verification without accessing real files. */
class SaveBuiltCircuitInteractorTest {

    @Test
    void savesAndReloadsBeforeReportingSuccess() {
        Fixture fixture = new Fixture();

        fixture.interactor().execute("example.circ");

        assertEquals("example.circ", fixture.writtenPath);
        assertEquals("example.circ", fixture.loadedPath);
        assertEquals("example.circ", fixture.successPath);
        assertEquals("", fixture.error);
    }

    @Test
    void refusesToSaveBeforeACircuitHasBeenBuilt() {
        Fixture fixture = new Fixture();
        fixture.circuit = null;

        fixture.interactor().execute("example.circ");

        assertEquals("Build a circuit before saving.", fixture.error);
        assertEquals("", fixture.writtenPath);
        assertEquals("", fixture.loadedPath);
        assertEquals("", fixture.successPath);
    }

    @Test
    void refusesABlankDestination() {
        Fixture fixture = new Fixture();

        fixture.interactor().execute(" ");

        assertEquals("Choose where to save the circuit.", fixture.error);
        assertEquals("", fixture.writtenPath);
    }

    @Test
    void refusesAnIncorrectFileExtension() {
        Fixture fixture = new Fixture();

        fixture.interactor().execute("example.txt");

        assertEquals("Choose a file name ending in .circ.", fixture.error);
        assertEquals("", fixture.writtenPath);
    }

    @Test
    void reportsWriteFailureWithoutReloading() {
        Fixture fixture = new Fixture();
        fixture.writer = (circuit, path) -> {
            throw new CircuitFileWriteException("Disk unavailable.", null);
        };

        fixture.interactor().execute("example.circ");

        assertTrue(fixture.error.contains("Disk unavailable."));
        assertEquals("", fixture.loadedPath);
        assertEquals("", fixture.successPath);
    }

    @Test
    void reportsReloadFailureWithoutClaimingSuccess() {
        Fixture fixture = new Fixture();
        fixture.reader = path -> {
            throw new CircuitParseException("E001", "Invalid exported file.");
        };

        fixture.interactor().execute("example.circ");

        assertEquals("example.circ", fixture.writtenPath);
        assertTrue(fixture.error.contains("Invalid exported file."));
        assertEquals("", fixture.successPath);
    }

    @Test
    void rejectsAnExportWhoseBehaviourChanged() {
        Fixture fixture = new Fixture();
        fixture.reloaded = constantOutputCircuit();

        fixture.interactor().execute("example.circ");

        assertTrue(fixture.error.contains("truth table differs"));
        assertEquals("", fixture.successPath);
    }

    @Test
    void reportsSuccessOnlyAfterTheReaderReturns() {
        Fixture fixture = new Fixture();
        fixture.reader = path -> {
            assertEquals("example.circ", fixture.writtenPath);
            assertEquals("", fixture.successPath);
            return fixture.reloaded;
        };

        fixture.interactor().execute("example.circ");

        assertFalse(fixture.successPath.isEmpty());
        assertEquals("", fixture.error);
    }

    private static Circuit passThroughCircuit() {
        return new Circuit(
                "Pass through",
                List.of(
                        new InputPin("a", "A", 0,
                                new Point(100, 100), Orientation.EAST),
                        new OutputPin("y", "Y", 0,
                                new Point(300, 100), Orientation.WEST)),
                List.of(),
                1,
                List.of());
    }

    private static Circuit constantOutputCircuit() {
        return new Circuit(
                "Changed circuit",
                List.of(
                        new InputPin("a", "A", 0,
                                new Point(100, 100), Orientation.EAST),
                        new Constant("zero", false, 1,
                                new Point(200, 100), Orientation.EAST),
                        new OutputPin("y", "Y", 1,
                                new Point(300, 100), Orientation.WEST)),
                List.of(),
                2,
                List.of());
    }

    private static final class Fixture implements
            BuiltCircuitDataAccessInterface, SaveBuiltCircuitOutputBoundary {
        private Circuit circuit = passThroughCircuit();
        private Circuit reloaded = passThroughCircuit();
        private String writtenPath = "";
        private String loadedPath = "";
        private String successPath = "";
        private String error = "";

        private CircuitFileWriterInterface writer = (value, path) -> {
            assertEquals(circuit, value);
            writtenPath = path;
        };

        private CircuitFileDataAccessInterface reader = path -> {
            loadedPath = path;
            return reloaded;
        };

        private SaveBuiltCircuitInteractor interactor() {
            return new SaveBuiltCircuitInteractor(
                    this,
                    writer,
                    reader,
                    new TruthTableGenerator(new CircuitSimulator()),
                    this);
        }

        @Override
        public void setBuiltCircuit(Circuit value) {
            circuit = value;
        }

        @Override
        public Optional<Circuit> getBuiltCircuit() {
            return Optional.ofNullable(circuit);
        }

        @Override
        public void prepareSuccessView(String path) {
            successPath = path;
        }

        @Override
        public void prepareFailView(String message) {
            error = message;
        }
    }
}
