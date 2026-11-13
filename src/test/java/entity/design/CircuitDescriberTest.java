package entity.design;

import entity.circuit.Circuit;
import entity.circuit.CircuitTestFactory;
import entity.circuit.GateInput;
import entity.circuit.GateType;
import entity.circuit.Gate;
import entity.circuit.InputPin;
import entity.circuit.Orientation;
import entity.circuit.OutputPin;
import entity.circuit.Point;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitDescriberTest {

    private final CircuitDescriber describer = new CircuitDescriber();

    @Test
    void describesHalfAdder() throws NotDescribableException {
        CircuitDesign design = describer.describe(CircuitTestFactory.halfAdder());

        CircuitDesign expected = new CircuitDesign("half adder", List.of("A", "B"), List.of("Cout", "Sum"),
                List.of(new GateSpec("g1", GateType.XOR, List.of("A", "B")),
                        new GateSpec("g2", GateType.AND, List.of("A", "B"))),
                Map.of("Sum", "g1", "Cout", "g2"));
        assertEquals(expected, design);
    }

    @Test
    void namesGatesLeftToRightThenTopToBottom() throws NotDescribableException {
        Circuit circuit = new Circuit("order", List.of(
                new InputPin("a", "A", 0, new Point(0, 0), Orientation.EAST),
                new Gate("right", GateType.NOT, List.of(new GateInput(1, false)), 2,
                        new Point(200, 0), Orientation.EAST),
                new Gate("lowLeft", GateType.NOT, List.of(new GateInput(0, false)), 3, new Point(100, 50),
                        Orientation.EAST),
                new Gate("highLeft", GateType.NOT, List.of(new GateInput(3, false)), 1, new Point(100, 10),
                        Orientation.EAST),
                new OutputPin("y", "Y", 2, new Point(300, 0), Orientation.WEST)), List.of(), 4, List.of());

        CircuitDesign design = describer.describe(circuit);

        assertEquals(List.of("g1", "g2", "g3"), design.gates().stream().map(GateSpec::id).toList());
        assertEquals(List.of("g2"), design.gates().get(0).inputs());
        assertEquals(List.of("A"), design.gates().get(1).inputs());
        assertEquals(List.of("g1"), design.gates().get(2).inputs());
        assertEquals("g3", design.connections().get("Y"));
    }

    @Test
    void keepsNegationAndConstants() throws NotDescribableException {
        Circuit circuit = CircuitTestFactory.builder("c")
                .input("A").constant("one", true).constant("zero", false)
                .gate("x", GateType.AND, "!A", "one")
                .output("Y", "x").output("Z", "zero").output("W", "A")
                .build();

        CircuitDesign design = describer.describe(circuit);

        assertEquals(List.of("!A", "1"), design.gates().get(0).inputs());
        assertEquals("0", design.connections().get("Z"));
        assertEquals("A", design.connections().get("W"));
    }

    @Test
    void skipsGateNamesUsedByPins() throws NotDescribableException {
        Circuit circuit = CircuitTestFactory.builder("c")
                .input("g1")
                .gate("x", GateType.NOT, "g1")
                .output("Y", "x")
                .build();

        CircuitDesign design = describer.describe(circuit);

        assertEquals("g2", design.gates().get(0).id());
        assertEquals(List.of("g1"), design.gates().get(0).inputs());
    }

    @Test
    void rejectsUnsupportedComponents() {
        Circuit circuit = CircuitTestFactory.builder("c")
                .input("A").output("Y", "A").unsupported("D Flip-Flop").build();

        assertMessage("does not support: D Flip-Flop", circuit);
    }

    @Test
    void rejectsFloatingGateInputAndUnconnectedOutput() {
        Circuit floatingInput = CircuitTestFactory.builder("c")
                .input("A").gate("x", GateType.AND, "A", "nothing").output("Y", "x").build();
        Circuit floatingOutput = CircuitTestFactory.builder("c").input("A").output("Y", "nothing").build();

        assertMessage("AND gate at (0,10) has an unconnected input", floatingInput);
        assertMessage("output Y is not connected to anything", floatingOutput);
    }

    @Test
    void rejectsWiresWithSeveralDrivers() {
        Circuit circuit = new Circuit("c", List.of(
                new InputPin("a", "A", 0, new Point(0, 0), Orientation.EAST),
                new InputPin("b", "B", 0, new Point(0, 20), Orientation.EAST),
                new OutputPin("y", "Y", 0, new Point(50, 0), Orientation.WEST)), List.of(), 1, List.of());

        assertMessage("several components drive the same wire", circuit);
    }

    @Test
    void rejectsLabelsThatAreNotValidNames() {
        Circuit circuit = CircuitTestFactory.builder("c").input("A").output("my out", "A").build();

        assertMessage("output label \"my out\" is not a valid name", circuit);
    }

    private void assertMessage(String expectedPart, Circuit circuit) {
        NotDescribableException e = assertThrows(NotDescribableException.class, () -> describer.describe(circuit));
        assertTrue(e.getMessage().contains(expectedPart), () -> "message was: " + e.getMessage());
    }
}
