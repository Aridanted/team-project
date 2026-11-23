package data_access.circ;

import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.GateType;
import entity.circuit.Orientation;
import entity.circuit.Point;
import entity.circuit.Wire;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WirePathFinderTest {
    private final WireRoutingRules rules = new WireRoutingRules();
    private final GateObstacleChecker obstacles = new GateObstacleChecker(new GatePortLocator());
    private final WirePathFinder finder = new WirePathFinder(rules, obstacles);

    @Test
    void mergesADirectRouteIntoOneSegment() {
        Point source = new Point(100, 200);
        Point destination = new Point(300, 200);
        Map<Integer, List<Point>> ports = Map.of(0, List.of(source, destination));

        List<Wire> path = finder.findPath(source, destination, 0, List.of(), ports, List.of());

        assertEquals(List.of(new Wire(source, destination, 0)), path);
    }

    @Test
    void crossesAnotherWireThroughItsInterior() {
        Point source = new Point(100, 200);
        Point destination = new Point(300, 200);
        Wire existing = new Wire(new Point(200, 100), new Point(200, 300), 1);
        Map<Integer, List<Point>> ports = Map.of(
                0, List.of(source, destination),
                1, List.of(existing.from(), existing.to()));

        List<Wire> path = finder.findPath(source, destination, 0,
                List.of(existing), ports, List.of());

        assertEquals(List.of(new Wire(source, destination, 0)), path);
        assertSafePath(path, source, destination, List.of(existing), ports, List.of());
    }

    @Test
    void detoursAroundAnotherWiresEndpoint() {
        Point source = new Point(100, 200);
        Point destination = new Point(300, 200);
        Wire existing = new Wire(new Point(200, 100), new Point(200, 200), 1);
        Map<Integer, List<Point>> ports = Map.of(
                0, List.of(source, destination),
                1, List.of(existing.from(), existing.to()));

        List<Wire> path = finder.findPath(source, destination, 0,
                List.of(existing), ports, List.of());

        assertTrue(path.stream().anyMatch(wire ->
                wire.from().y() != 200 || wire.to().y() != 200));
        assertSafePath(path, source, destination, List.of(existing), ports, List.of());
    }

    @Test
    void routesAroundAGateBody() {
        Point source = new Point(100, 200);
        Point destination = new Point(580, 200);
        Gate gate = new Gate("G1", GateType.NOT, List.of(new GateInput(1, false)),
                2, new Point(340, 200), Orientation.EAST);
        Map<Integer, List<Point>> ports = Map.of(
                0, List.of(source, destination),
                1, List.of(new Point(310, 200)),
                2, List.of(gate.location()));
        List<Gate> gates = List.of(gate);

        List<Wire> path = finder.findPath(source, destination, 0,
                List.of(), ports, gates);

        assertTrue(path.stream().anyMatch(wire ->
                wire.from().y() != 200 || wire.to().y() != 200));
        assertSafePath(path, source, destination, List.of(), ports, gates);
    }

    @Test
    void reportsFailureWhenDifferentNetsShareTheSourcePosition() {
        Point source = new Point(100, 200);
        Point destination = new Point(300, 200);
        Map<Integer, List<Point>> ports = Map.of(
                0, List.of(source, destination),
                1, List.of(source));

        assertThrows(IllegalArgumentException.class,
                () -> finder.findPath(source, destination, 0, List.of(), ports, List.of()));
    }

    private void assertSafePath(List<Wire> path, Point source, Point destination,
                                List<Wire> existing, Map<Integer, List<Point>> ports,
                                List<Gate> gates) {
        assertFalse(path.isEmpty());
        assertEquals(source, path.getFirst().from());
        assertEquals(destination, path.getLast().to());

        Point previous = source;
        for (Wire wire : path) {
            assertEquals(previous, wire.from());
            assertEquals(0, wire.net());
            assertEquals(0, wire.from().x() % 10);
            assertEquals(0, wire.from().y() % 10);
            assertEquals(0, wire.to().x() % 10);
            assertEquals(0, wire.to().y() % 10);
            assertTrue(rules.canPlace(wire, existing, ports));
            assertTrue(obstacles.canPlace(wire, gates));
            previous = wire.to();
        }
    }
}
