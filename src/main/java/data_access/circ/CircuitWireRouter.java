package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.Point;
import entity.circuit.Wire;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Routes every net in a positioned circuit. */
public final class CircuitWireRouter {
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
     * Routes all nets from their source ports to their destination ports.
     *
     * @param circuit the positioned circuit with normalized unary inputs
     * @return the circuit with a newly generated wire list
     * @throws IllegalArgumentException if a connection cannot be routed safely
     */
    public Circuit route(Circuit circuit) {
        Map<Integer, List<Point>> portsByNet = portCollector.collect(circuit);
        List<Wire> wires = new ArrayList<>();

        List<Integer> nets = portsByNet.keySet().stream().sorted().toList();
        for (int net : nets) {
            List<Point> ports = portsByNet.get(net);
            Point source = ports.getFirst();

            for (int index = 1; index < ports.size(); index++) {
                Point destination = ports.get(index);
                try {
                    wires.addAll(pathFinder.findPath(source, destination, net,
                            wires, portsByNet, circuit.gates()));
                } catch (IllegalArgumentException error) {
                    throw new IllegalArgumentException(
                            "Could not route net " + net + ". " + error.getMessage(), error);
                }
            }
        }

        return new Circuit(circuit.name(), circuit.components(), List.copyOf(wires),
                circuit.netCount(), circuit.warnings());
    }
}
