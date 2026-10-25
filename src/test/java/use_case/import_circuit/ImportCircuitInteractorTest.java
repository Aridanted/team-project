package use_case.import_circuit;

import entity.circuit.Circuit;
import entity.circuit.CircuitTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportCircuitInteractorTest {

    private RecordingSession session;
    private RecordingPresenter presenter;
    private ImportCircuitInteractor interactor;

    @BeforeEach
    void setUp() {
        session = new RecordingSession();
        presenter = new RecordingPresenter();
        interactor = new ImportCircuitInteractor(new FakeCircuitFiles(), session, presenter);
    }

    @Test
    void successLoadsHalfAdderAndUpdatesSession() {
        interactor.execute(new ImportCircuitInputData("/circuits/half.circ"));

        assertEquals("half adder", session.current.get().name());
        ImportCircuitOutputData out = presenter.lastSuccess;
        assertEquals("half adder", out.circuitName());
        assertEquals(List.of("A", "B"), out.inputLabels());
        assertEquals(List.of("Cout", "Sum"), out.outputLabels());
        assertTrue(out.warnings().isEmpty());
        assertTrue(out.simulatable());
        assertEquals(6, out.components().size());
        assertComponent(out.components().get(0), "input", "", "A", 0, 0, "east");
        assertComponent(out.components().get(1), "input", "", "B", 0, 10, "east");
        assertComponent(out.components().get(2), "gate", "XOR", "", 0, 20, "east");
        assertComponent(out.components().get(3), "gate", "AND", "", 0, 30, "east");
        assertComponent(out.components().get(4), "output", "", "Sum", 0, 40, "west");
        assertComponent(out.components().get(5), "output", "", "Cout", 0, 50, "west");
        assertTrue(out.wires().isEmpty());
        assertNull(presenter.lastFailure);
    }

    @Test
    void successWithUnsupportedComponentAndWarning() {
        Circuit circuit = CircuitTestFactory.builder("broken")
                .input("A")
                .unsupported("D Flip-Flop")
                .warning("W101", "flip-flop ignored")
                .build();
        interactor = new ImportCircuitInteractor(
                path -> circuit, session, presenter);

        interactor.execute(new ImportCircuitInputData("x.circ"));

        assertSame(circuit, session.current.get());
        ImportCircuitOutputData out = presenter.lastSuccess;
        assertFalse(out.simulatable());
        assertEquals(List.of("flip-flop ignored"), out.warnings());
        assertEquals(2, out.components().size());
        assertComponent(out.components().get(0), "input", "", "A", 0, 0, "east");
        assertComponent(out.components().get(1), "unsupported", "D Flip-Flop", "", 0, 10, "east");
    }

    @Test
    void parseFailureShowsMessageAndLeavesSessionUntouched() {
        Circuit previous = CircuitTestFactory.halfAdder();
        session.current = Optional.of(previous);
        interactor = new ImportCircuitInteractor(
                path -> {
                    throw new CircuitParseException("E001", "not a circuit");
                },
                session,
                presenter);

        interactor.execute(new ImportCircuitInputData("bad.circ"));

        assertSame(previous, session.current.get());
        assertEquals("not a circuit", presenter.lastFailure);
        assertNull(presenter.lastSuccess);
        assertEquals(0, session.setCount);
    }

    @Test
    void blankPathFailsWithoutTouchingSession() {
        interactor.execute(new ImportCircuitInputData("   "));

        assertEquals("Choose a .circ file to open.", presenter.lastFailure);
        assertTrue(session.current.isEmpty());
    }

    @Test
    void nullPathFailsWithoutTouchingSession() {
        interactor.execute(new ImportCircuitInputData(null));

        assertEquals("Choose a .circ file to open.", presenter.lastFailure);
        assertTrue(session.current.isEmpty());
    }

    @Test
    void wrongExtensionFails() {
        interactor.execute(new ImportCircuitInputData("diagram.txt"));

        assertEquals("Only Logisim .circ files can be opened.", presenter.lastFailure);
        assertTrue(session.current.isEmpty());
    }

    @Test
    void uppercaseCircExtensionAccepted() {
        interactor.execute(new ImportCircuitInputData("/path/HALF.CIRC"));

        assertTrue(session.current.isPresent());
        assertEquals("half adder", presenter.lastSuccess.circuitName());
    }

    private static void assertComponent(ComponentSummary component, String kind, String detail,
                                        String label, int x, int y, String facing) {
        assertEquals(kind, component.kind());
        assertEquals(detail, component.detail());
        assertEquals(label, component.label());
        assertEquals(x, component.x());
        assertEquals(y, component.y());
        assertEquals(facing, component.facing());
    }

    private static final class FakeCircuitFiles implements CircuitFileDataAccessInterface {
        @Override
        public Circuit load(String path) {
            return CircuitTestFactory.halfAdder();
        }
    }

    private static final class RecordingSession implements ImportCircuitDataAccessInterface {
        Optional<Circuit> current = Optional.empty();
        int setCount;

        @Override
        public void setCurrentCircuit(Circuit circuit) {
            setCount++;
            current = Optional.of(circuit);
        }
    }

    private static final class RecordingPresenter implements ImportCircuitOutputBoundary {
        ImportCircuitOutputData lastSuccess;
        String lastFailure;

        @Override
        public void prepareSuccessView(ImportCircuitOutputData outputData) {
            lastSuccess = outputData;
            lastFailure = null;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            lastFailure = errorMessage;
            lastSuccess = null;
        }
    }
}
