package entity.circuit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitTest {

    private static final Point P = new Point(0, 0);

    private static InputPin in(String id, String label, int net) {
        return new InputPin(id, label, net, P, Orientation.EAST);
    }

    private static OutputPin out(String id, String label, int net) {
        return new OutputPin(id, label, net, P, Orientation.WEST);
    }

    private static Gate and(String id, int a, int b, int output) {
        return new Gate(id, GateType.AND, List.of(new GateInput(a, false), new GateInput(b, false)),
                output, P, Orientation.EAST);
    }

    @Test
    void pinsAreSortedNaturallyByLabel() {
        Circuit circuit = new Circuit("c", List.of(in("1", "IN10", 0), in("2", "IN2", 1), in("3", "a", 2),
                out("4", "Y", 0)), List.of(), 3, List.of());

        assertEquals(List.of("a", "IN2", "IN10"), circuit.inputPins().stream().map(Pin::label).toList());
        assertEquals(List.of("Y"), circuit.outputPins().stream().map(Pin::label).toList());
    }

    @Test
    void exposesGatesAndSimulatability() {
        Circuit supported = new Circuit("c", List.of(in("a", "A", 0), in("b", "B", 1), and("g", 0, 1, 2),
                out("y", "Y", 2)), List.of(), 3, List.of());
        assertEquals(1, supported.gates().size());
        assertTrue(supported.isSimulatable());

        Circuit unsupported = new Circuit("c", List.of(new UnsupportedComponent("u", "D Flip-Flop", "",
                P, Orientation.EAST)), List.of(), 0, List.of());
        assertFalse(unsupported.isSimulatable());
        assertEquals(1, unsupported.unsupportedComponents().size());
    }

    @Test
    void rejectsDuplicateIds() {
        assertThrows(IllegalArgumentException.class, () -> new Circuit("c",
                List.of(in("x", "A", 0), in("x", "B", 1)), List.of(), 2, List.of()));
    }

    @Test
    void rejectsNetsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new Circuit("c",
                List.of(in("a", "A", 5)), List.of(), 2, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Circuit("c",
                List.of(), List.of(new Wire(P, new Point(10, 0), 3)), 2, List.of()));
    }

    @Test
    void rejectsDuplicateLabelsIgnoringCase() {
        assertThrows(IllegalArgumentException.class, () -> new Circuit("c",
                List.of(in("a", "Sum", 0), out("b", "SUM", 0)), List.of(), 1, List.of()));
    }

    @Test
    void rejectsBlankLabels() {
        assertThrows(IllegalArgumentException.class, () -> new Circuit("c",
                List.of(in("a", " ", 0)), List.of(), 1, List.of()));
    }

    @Test
    void isImmutable() {
        Circuit circuit = new Circuit("c", List.of(in("a", "A", 0)), List.of(), 1, List.of());
        assertThrows(UnsupportedOperationException.class, () -> circuit.components().clear());
    }

    @Test
    void componentsReportTheirNets() {
        Gate gate = and("g", 0, 1, 2);
        assertEquals(List.of(0, 1), gate.inputNets());
        assertEquals(2, gate.drivenNet().getAsInt());
        assertEquals(List.of(4), out("o", "Y", 4).inputNets());
        assertTrue(out("o", "Y", 4).drivenNet().isEmpty());
        assertEquals(7, in("i", "A", 7).drivenNet().getAsInt());
    }

    @Test
    void unaryGatesTakeExactlyOneInput() {
        assertThrows(IllegalArgumentException.class, () -> new Gate("n", GateType.NOT,
                List.of(new GateInput(0, false), new GateInput(1, false)), 2, P, Orientation.EAST));
        assertThrows(IllegalArgumentException.class, () -> new Gate("n", GateType.AND,
                List.of(), 2, P, Orientation.EAST));
    }
}
