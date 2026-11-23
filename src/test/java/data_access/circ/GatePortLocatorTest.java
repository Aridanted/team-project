package data_access.circ;

import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.GateType;
import entity.circuit.Orientation;
import entity.circuit.Point;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatePortLocatorTest {
    private final GatePortLocator locator = new GatePortLocator();

    @Test
    void locatesTheInputsOfAnEastFacingAndGate() {
        Gate gate = binaryGate(GateType.AND, Orientation.EAST, false);

        assertEquals(List.of(new Point(290, 180), new Point(290, 220)),
                locator.inputPorts(gate));
    }

    @Test
    void movesANegatedInputPortByTheBubbleWidth() {
        Gate gate = binaryGate(GateType.AND, Orientation.EAST, true);

        assertEquals(List.of(new Point(280, 180), new Point(290, 220)),
                locator.inputPorts(gate));
    }

    @Test
    void accountsForTheExtraWidthOfAnXnorGate() {
        Gate gate = binaryGate(GateType.XNOR, Orientation.EAST, false);

        assertEquals(List.of(new Point(270, 180), new Point(270, 220)),
                locator.inputPorts(gate));
    }

    @Test
    void rotatesInputPortsForANorthFacingGate() {
        Gate gate = binaryGate(GateType.AND, Orientation.NORTH, false);

        assertEquals(List.of(new Point(320, 250), new Point(360, 250)),
                locator.inputPorts(gate));
    }

    @Test
    void locatesTheInputOfANotGate() {
        Gate gate = new Gate("G1", GateType.NOT, List.of(new GateInput(0, false)),
                1, new Point(340, 200), Orientation.EAST);

        assertEquals(List.of(new Point(310, 200)), locator.inputPorts(gate));
    }

    @Test
    void locatesTheInputOfABuffer() {
        Gate gate = new Gate("G1", GateType.BUFFER, List.of(new GateInput(0, false)),
                1, new Point(340, 200), Orientation.EAST);

        assertEquals(List.of(new Point(320, 200)), locator.inputPorts(gate));
    }

    @Test
    void rejectsAnUnnormalizedUnaryInput() {
        Gate gate = new Gate("G1", GateType.NOT, List.of(new GateInput(0, true)),
                1, new Point(340, 200), Orientation.EAST);

        assertThrows(IllegalArgumentException.class, () -> locator.inputPorts(gate));
    }

    private static Gate binaryGate(GateType type, Orientation facing, boolean firstInputNegated) {
        return new Gate("G1", type,
                List.of(new GateInput(0, firstInputNegated), new GateInput(1, false)),
                2, new Point(340, 200), facing);
    }
}
