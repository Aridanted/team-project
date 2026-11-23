package data_access.circ;

import entity.circuit.Point;
import entity.circuit.Wire;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireRoutingRulesTest {
    private final WireRoutingRules rules = new WireRoutingRules();

    @Test
    void allowsAnInteriorCrossingBetweenDifferentNets() {
        Wire horizontal = wire(100, 200, 300, 200, 0);
        Wire vertical = wire(200, 100, 200, 300, 1);

        assertTrue(rules.canPlace(vertical, List.of(horizontal), Map.of()));
    }

    @Test
    void rejectsAnEndpointTouchingAnotherNet() {
        Wire horizontal = wire(100, 200, 300, 200, 0);
        Wire vertical = wire(200, 100, 200, 200, 1);

        assertFalse(rules.canPlace(vertical, List.of(horizontal), Map.of()));
    }

    @Test
    void rejectsAnExistingEndpointInsideTheNewSegment() {
        Wire vertical = wire(200, 100, 200, 200, 0);
        Wire horizontal = wire(100, 200, 300, 200, 1);

        assertFalse(rules.canPlace(horizontal, List.of(vertical), Map.of()));
    }

    @Test
    void rejectsCollinearOverlapBetweenDifferentNets() {
        Wire existing = wire(100, 200, 300, 200, 0);
        Wire candidate = wire(200, 200, 400, 200, 1);

        assertFalse(rules.canPlace(candidate, List.of(existing), Map.of()));
    }

    @Test
    void allowsAJunctionOnTheSameNet() {
        Wire horizontal = wire(100, 200, 300, 200, 0);
        Wire vertical = wire(200, 100, 200, 200, 0);

        assertTrue(rules.canPlace(vertical, List.of(horizontal), Map.of()));
    }

    @Test
    void rejectsAWirePassingThroughAnotherNetsPort() {
        Wire candidate = wire(100, 200, 300, 200, 0);
        Map<Integer, List<Point>> ports = Map.of(1, List.of(new Point(200, 200)));

        assertFalse(rules.canPlace(candidate, List.of(), ports));
    }

    @Test
    void allowsAWirePassingThroughItsOwnPort() {
        Wire candidate = wire(100, 200, 300, 200, 0);
        Map<Integer, List<Point>> ports = Map.of(0, List.of(new Point(200, 200)));

        assertTrue(rules.canPlace(candidate, List.of(), ports));
    }

    @Test
    void allowsDisjointParallelSegments() {
        Wire existing = wire(100, 200, 300, 200, 0);
        Wire candidate = wire(100, 300, 300, 300, 1);

        assertTrue(rules.canPlace(candidate, List.of(existing), Map.of()));
    }

    @Test
    void rejectsADiagonalSegment() {
        Wire candidate = wire(100, 100, 200, 200, 0);

        assertThrows(IllegalArgumentException.class,
                () -> rules.canPlace(candidate, List.of(), Map.of()));
    }

    @Test
    void rejectsAZeroLengthSegment() {
        Wire candidate = wire(100, 100, 100, 100, 0);

        assertThrows(IllegalArgumentException.class,
                () -> rules.canPlace(candidate, List.of(), Map.of()));
    }

    private static Wire wire(int fromX, int fromY, int toX, int toY, int net) {
        return new Wire(new Point(fromX, fromY), new Point(toX, toY), net);
    }
}
