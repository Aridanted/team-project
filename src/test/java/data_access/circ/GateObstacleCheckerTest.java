package data_access.circ;

import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.GateType;
import entity.circuit.Orientation;
import entity.circuit.Point;
import entity.circuit.Wire;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateObstacleCheckerTest {
    private final GateObstacleChecker checker = new GateObstacleChecker(new GatePortLocator());

    private final Gate gate = new Gate("G1", GateType.AND,
            List.of(new GateInput(0, false), new GateInput(1, false)),
            2, new Point(340, 200), Orientation.EAST);

    @Test
    void rejectsAHorizontalWireThroughTheGate() {
        assertFalse(checker.canPlace(wire(250, 200, 400, 200), List.of(gate)));
    }

    @Test
    void rejectsAVerticalWireThroughTheGate() {
        assertFalse(checker.canPlace(wire(310, 100, 310, 300), List.of(gate)));
    }

    @Test
    void rejectsAWireWhollyInsideTheGate() {
        assertFalse(checker.canPlace(wire(300, 200, 320, 200), List.of(gate)));
    }

    @Test
    void allowsAWireEndingAtAnInputPort() {
        assertTrue(checker.canPlace(wire(200, 180, 290, 180), List.of(gate)));
    }

    @Test
    void allowsAWireStartingAtTheOutputPort() {
        assertTrue(checker.canPlace(wire(340, 200, 450, 200), List.of(gate)));
    }

    @Test
    void allowsAWireAboveTheGate() {
        assertTrue(checker.canPlace(wire(250, 150, 400, 150), List.of(gate)));
    }

    @Test
    void rejectsAGateThatHasNotBeenOrientedByTheLayout() {
        Gate northFacing = new Gate("G2", GateType.NOT,
                List.of(new GateInput(0, false)),
                1, new Point(340, 200), Orientation.NORTH);

        assertThrows(IllegalArgumentException.class,
                () -> checker.canPlace(wire(100, 100, 200, 100), List.of(northFacing)));
    }

    @Test
    void allowsAccessToAnInputBesideAnotherInputsBubble() {
        Gate negatedGate = new Gate("G2", GateType.AND,
                List.of(new GateInput(0, true), new GateInput(1, false)),
                2, new Point(340, 200), Orientation.EAST);

        assertTrue(checker.canPlace(wire(200, 220, 290, 220), List.of(negatedGate)));
    }

    private static Wire wire(int fromX, int fromY, int toX, int toY) {
        return new Wire(new Point(fromX, fromY), new Point(toX, toY), 0);
    }
}
