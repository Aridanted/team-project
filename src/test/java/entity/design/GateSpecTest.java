package entity.design;

import entity.circuit.GateType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static entity.design.CircuitDesignTest.assertMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateSpecTest {

    @Test
    void keepsInputsInOrder() {
        GateSpec gate = new GateSpec("g1", GateType.OR, List.of("!A", "B", "0"));

        assertEquals(List.of("!A", "B", "0"), gate.inputs());
    }

    @Test
    void unaryGatesNeedExactlyOneInput() {
        assertMessage("NOT gate and needs exactly 1 input, not 2",
                () -> new GateSpec("g1", GateType.NOT, List.of("A", "B")));
        assertEquals(1, new GateSpec("g1", GateType.BUFFER, List.of("A")).inputs().size());
    }

    @Test
    void multiInputGatesNeedTwoToThirtyTwoInputs() {
        assertMessage("AND gate and needs 2 to 32 inputs, not 1", () -> new GateSpec("g1", GateType.AND, List.of("A")));
        assertMessage("not 33", () -> new GateSpec("g1", GateType.AND, Collections.nCopies(33, "A")));
    }

    @Test
    void rejectsInvalidIdAndMalformedReferences() {
        assertMessage("gate id \"g 1\" is not a valid name", () -> new GateSpec("g 1", GateType.NOT, List.of("A")));
        assertMessage("has input \"!!A\"", () -> new GateSpec("g1", GateType.NOT, List.of("!!A")));
        assertMessage("has input \"\"", () -> new GateSpec("g1", GateType.NOT, List.of("")));
        assertMessage("has input \"2\"", () -> new GateSpec("g1", GateType.NOT, List.of("2")));
    }

    @Test
    void signalHelpers() {
        assertTrue(Signals.isNegated("!B"));
        assertEquals("B", Signals.signalOf("!B"));
        assertEquals("!B", Signals.reference("B", true));
        assertEquals("B", Signals.reference("B", false));
        assertTrue(Signals.isConstant("1"));
        assertFalse(Signals.isValidName("x".repeat(33)));
        assertTrue(Signals.isValidName("Cin_2"));
    }
}
