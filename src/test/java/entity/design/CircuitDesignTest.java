package entity.design;

import entity.circuit.GateType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitDesignTest {

    static CircuitDesign halfAdder() {
        return new CircuitDesign("half adder", List.of("A", "B"), List.of("Sum", "Cout"),
                List.of(new GateSpec("g1", GateType.XOR, List.of("A", "B")),
                        new GateSpec("g2", GateType.AND, List.of("A", "B"))),
                Map.of("Sum", "g1", "Cout", "g2"));
    }

    @Test
    void validDesignKeepsConnectionsInOutputOrder() {
        CircuitDesign design = halfAdder();

        assertEquals(List.of("Sum", "Cout"), List.copyOf(design.connections().keySet()));
        assertEquals("g1", design.connections().get("Sum"));
        assertEquals(GateType.AND, design.gate("g2").orElseThrow().type());
        assertTrue(design.gate("g3").isEmpty());
    }

    @Test
    void nameIsStripped() {
        CircuitDesign design = new CircuitDesign("  mine ", List.of("A"), List.of("Y"), List.of(), Map.of("Y", "A"));

        assertEquals("mine", design.name());
    }

    @Test
    void negatedInputsAndConstantsAreAllowed() {
        CircuitDesign design = new CircuitDesign("c", List.of("A"), List.of("Y", "Z"),
                List.of(new GateSpec("g1", GateType.AND, List.of("!A", "1"))), Map.of("Y", "g1", "Z", "0"));

        assertEquals(List.of("!A", "1"), design.gates().get(0).inputs());
        assertEquals("0", design.connections().get("Z"));
    }

    @Test
    void gatesMayUseGatesDefinedLater() {
        CircuitDesign design = new CircuitDesign("c", List.of("A"), List.of("Y"),
                List.of(
                        new GateSpec("g1", GateType.NOT, List.of("g2")),
                        new GateSpec("g2", GateType.NOT, List.of("A"))),
                Map.of("Y", "g1"));

        assertEquals(2, design.gates().size());
    }

    @Test
    void equalityIgnoresConnectionMapOrder() {
        Map<String, String> reversed = new LinkedHashMap<>();
        reversed.put("Cout", "g2");
        reversed.put("Sum", "g1");
        CircuitDesign other = new CircuitDesign("half adder", List.of("A", "B"), List.of("Sum", "Cout"),
                halfAdder().gates(), reversed);

        assertEquals(halfAdder(), other);
    }

    @Test
    void rejectsBlankOrLongName() {
        assertMessage("needs a name", () -> design(" ", List.of("A"), List.of("Y"), List.of(), Map.of("Y", "A")));
        assertMessage("longer than 60", () -> design("x".repeat(61), List.of("A"), List.of("Y"), List.of(),
                Map.of("Y", "A")));
    }

    @Test
    void rejectsTooFewOrTooManySignals() {
        assertMessage("1 to 10 inputs, not 0", () -> design("c", List.of(), List.of("Y"), List.of(), Map.of("Y", "0")));
        assertMessage("1 to 10 inputs, not 11", () -> design("c", names("I", 11), List.of("Y"), List.of(),
                Map.of("Y", "I1")));
        assertMessage("1 to 8 outputs, not 0", () -> design("c", List.of("A"), List.of(), List.of(), Map.of()));
        List<String> outputs = names("O", 9);
        Map<String, String> connections = new LinkedHashMap<>();
        outputs.forEach(output -> connections.put(output, "A"));
        assertMessage("1 to 8 outputs, not 9", () -> design("c", List.of("A"), outputs, List.of(), connections));
    }

    @Test
    void rejectsMoreThanFiftyGates() {
        List<GateSpec> gates = new ArrayList<>();
        for (int i = 1; i <= 51; i++) {
            gates.add(new GateSpec("g" + i, GateType.NOT, List.of("A")));
        }

        assertMessage("0 to 50 gates, not 51", () -> design("c", List.of("A"), List.of("Y"), gates, Map.of("Y", "g1")));
    }

    @Test
    void rejectsInvalidLabels() {
        assertMessage("input label \"2A\" is not a valid name",
                () -> design("c", List.of("2A"), List.of("Y"), List.of(), Map.of("Y", "0")));
        assertMessage("output label \"my out\" is not a valid name",
                () -> design("c", List.of("A"), List.of("my out"), List.of(), Map.of("my out", "A")));
    }

    @Test
    void rejectsNamesThatDifferOnlyInCase() {
        assertMessage("the name a is used twice (as input A and as input a)",
                () -> design("c", List.of("A", "a"), List.of("Y"), List.of(), Map.of("Y", "A")));
        assertMessage("used twice (as input A and as gate a)", () -> design("c", List.of("A"), List.of("Y"),
                List.of(new GateSpec("a", GateType.NOT, List.of("A"))), Map.of("Y", "a")));
    }

    @Test
    void rejectsUndeclaredGateInput() {
        assertMessage("gate g1 uses C, which is not a declared input or gate", () -> design("c", List.of("A"),
                List.of("Y"), List.of(new GateSpec("g1", GateType.AND, List.of("A", "!C"))), Map.of("Y", "g1")));
    }

    @Test
    void namesMustMatchExactly() {
        assertMessage("gate g1 uses a, which is not a declared input or gate", () -> design("c", List.of("A"),
                List.of("Y"), List.of(new GateSpec("g1", GateType.NOT, List.of("a"))), Map.of("Y", "g1")));
    }

    @Test
    void rejectsGateReadingAnOutput() {
        assertMessage("gate g1 uses output Y as an input", () -> design("c", List.of("A"), List.of("Y"),
                List.of(new GateSpec("g1", GateType.NOT, List.of("Y"))), Map.of("Y", "g1")));
    }

    @Test
    void rejectsUnconnectedOutput() {
        assertMessage("output Cout is not connected", () -> design("c", List.of("A"), List.of("Sum", "Cout"),
                List.of(), Map.of("Sum", "A")));
    }

    @Test
    void rejectsConnectionForUnknownOutput() {
        assertMessage("connections names Q, which is not a declared output", () -> design("c", List.of("A"),
                List.of("Y"), List.of(), Map.of("Y", "A", "Q", "A")));
    }

    @Test
    void rejectsConnectionToUndeclaredOrNegatedSignal() {
        assertMessage("output Y is connected to B, which is not a declared input or gate",
                () -> design("c", List.of("A"), List.of("Y"), List.of(), Map.of("Y", "B")));
        assertMessage("outputs cannot be negated, add a NOT gate instead",
                () -> design("c", List.of("A"), List.of("Y"), List.of(), Map.of("Y", "!A")));
        assertMessage("output Y is connected to Z, which is not a declared input or gate",
                () -> design("c", List.of("A"), List.of("Y", "Z"), List.of(), Map.of("Y", "Z", "Z", "A")));
    }

    private static CircuitDesign design(String name, List<String> inputs, List<String> outputs, List<GateSpec> gates,
                                        Map<String, String> connections) {
        return new CircuitDesign(name, inputs, outputs, gates, connections);
    }

    private static List<String> names(String prefix, int count) {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            names.add(prefix + i);
        }
        return names;
    }

    static void assertMessage(String expectedPart, org.junit.jupiter.api.function.Executable executable) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertTrue(e.getMessage().contains(expectedPart), () -> "message was: " + e.getMessage());
    }
}
