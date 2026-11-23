package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.Point;
import entity.circuit.Wire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Routes nets while reserving access to every component port. */
public final class CircuitWireRouter {
    private static final int PORT_ACCESS_LENGTH = 10;

    private final CircuitPortCollector portCollector;
    private final WirePathFinder pathFinder;

    /**
     * Creates a router using the supplied port collector and path finder.
     *
     * @param portCollector the collector for circuit connection points
     * @param pathFinder the finder for safe orthogonal paths
     */
    public CircuitWireRouter(CircuitPortCollector portCollector, WirePathFinder pathFinder) {
        this.portCollector = Objects.requireNonNull(portCollector, "portCollector");
        this.pathFinder = Objects.requireNonNull(pathFinder, "pathFinder");
    }

    /**
     * Routes a circuit after layout and unary input normalization.
     *
     * @param circuit the positioned circuit with east-facing gates and sources
     * @return the circuit with newly generated wires
     * @throws IllegalArgumentException if a connection cannot be routed safely
     */
    public Circuit route(Circuit circuit) {
        Map<Integer, List<Point>> originalPorts = portCollector.collect(circuit);
        Map<Integer, List<Point>> protectedPorts = new HashMap<>();
        List<Wire> wires = new ArrayList<>();
        Map<Integer, List<Point>> terminals = reserveAccess(originalPorts, protectedPorts, wires);

        for (int net : terminals.keySet().stream().sorted().toList()) {
            routeNet(net, terminals.get(net), protectedPorts, wires, circuit);
        }

        return new Circuit(circuit.name(), circuit.components(), wires,
                circuit.netCount(), circuit.warnings());
    }

    private Map<Integer, List<Point>> reserveAccess(Map<Integer, List<Point>> originalPorts,
                                                    Map<Integer, List<Point>> protectedPorts,
                                                    List<Wire> wires) {
        Map<Integer, List<Point>> terminals = new HashMap<>();
        for (int net : originalPorts.keySet().stream().sorted().toList()) {
            List<Point> ports = originalPorts.get(net);
            if (ports.size() == 1) {
                terminals.put(net, ports);
                protectedPorts.put(net, ports);
                continue;
            }

            List<Point> ends = new ArrayList<>();
            List<Point> protectedPoints = new ArrayList<>(ports);
            for (int index = 0; index < ports.size(); index++) {
                Point port = ports.get(index);
                int offset = index == 0 ? PORT_ACCESS_LENGTH : -PORT_ACCESS_LENGTH;
                Point access = new Point(port.x() + offset, port.y());
                wires.add(new Wire(port, access, net));
                ends.add(access);
                protectedPoints.add(access);
            }
            terminals.put(net, List.copyOf(ends));
            protectedPorts.put(net, List.copyOf(protectedPoints));
        }
        return terminals;
    }

    private void routeNet(int net, List<Point> terminals, Map<Integer, List<Point>> protectedPorts,
                          List<Wire> wires, Circuit circuit) {
        List<Point> connected = new ArrayList<>();
        connected.add(terminals.getFirst());
        List<Point> destinations = terminals.subList(1, terminals.size()).stream()
                .sorted(Comparator.comparingInt(Point::y).thenComparingInt(Point::x))
                .toList();

        for (Point destination : destinations) {
            Point source = connected.stream()
                    .min(Comparator.comparingInt(point -> distance(point, destination)))
                    .orElseThrow();
            try {
                wires.addAll(pathFinder.findPath(source, destination, net,
                        wires, protectedPorts, circuit.gates()));
                connected.add(destination);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(
                        "Could not route net " + net + ". " + error.getMessage(), error);
            }
        }
    }

    private int distance(Point first, Point second) {
        return Math.abs(first.x() - second.x()) + Math.abs(first.y() - second.y());
    }
}
