package data_access.circ;

import entity.circuit.CircuitComponent;
import entity.circuit.CircuitWarning;
import entity.circuit.Gate;
import entity.circuit.OutputPin;
import entity.circuit.Point;
import entity.circuit.UnsupportedComponent;
import use_case.import_circuit.CircuitParseException;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks the wiring once nets are known: two drivers on one net is an error (E004); floating
 * inputs (W102), wires that stop just short of a port (W104) and very large circuits (W105) are
 * warnings.
 */
final class ConnectivityChecker {

    /** A domain component together with where its input ports are. */
    record Placement(CircuitComponent component, List<Point> inputPorts) {

        Placement {
            inputPorts = List.copyOf(inputPorts);
        }
    }

    static final int LARGE_CIRCUIT_COMPONENTS = 300;

    /** How close a loose wire end must be to an unattached port to be reported. */
    static final int NEAR_MISS_DISTANCE = 20;

    void check(List<Placement> placements, NetMap nets, List<CircuitWarning> warnings)
            throws CircuitParseException {
        Set<Integer> drivenNets = requireSingleDriverPerNet(placements);
        boolean hasUnsupported = placements.stream()
                .anyMatch(p -> p.component() instanceof UnsupportedComponent);
        if (!hasUnsupported) {
            // Unsupported components have unknown ports, so nets they drive would look floating.
            reportFloatingInputs(placements, drivenNets, warnings);
        }
        reportNearMisses(placements, nets, drivenNets, warnings);
        if (placements.size() > LARGE_CIRCUIT_COMPONENTS) {
            warnings.add(Diagnostics.largeCircuit(placements.size()));
        }
    }

    private static Set<Integer> requireSingleDriverPerNet(List<Placement> placements)
            throws CircuitParseException {
        Map<Integer, CircuitComponent> driverByNet = new HashMap<>();
        for (Placement placement : placements) {
            CircuitComponent component = placement.component();
            if (component.drivenNet().isPresent()
                    && driverByNet.putIfAbsent(component.drivenNet().getAsInt(), component) != null) {
                throw Diagnostics.contention(component.location());
            }
        }
        return driverByNet.keySet();
    }

    private static void reportFloatingInputs(List<Placement> placements, Set<Integer> drivenNets,
                                             List<CircuitWarning> warnings) {
        for (Placement placement : placements) {
            if (placement.component() instanceof Gate gate) {
                for (int i = 0; i < gate.inputs().size(); i++) {
                    if (!drivenNets.contains(gate.inputs().get(i).net())) {
                        warnings.add(Diagnostics.floatingGateInput(gate.type(), i, gate.location()));
                    }
                }
            } else if (placement.component() instanceof OutputPin pin && !drivenNets.contains(pin.net())) {
                warnings.add(Diagnostics.floatingOutputPin(pin.label(), pin.location()));
            }
        }
    }

    private static void reportNearMisses(List<Placement> placements, NetMap nets, Set<Integer> drivenNets,
                                         List<CircuitWarning> warnings) {
        Set<Point> reported = new LinkedHashSet<>();
        List<Point> unattachedPorts = placements.stream()
                .flatMap(p -> p.inputPorts().stream())
                .filter(port -> {
                    int net = nets.netAt(port);
                    return !nets.hasWire(net) && !drivenNets.contains(net);
                })
                .collect(Collectors.toList());
        for (Point port : unattachedPorts) {
            for (Point end : nets.danglingWireEnds()) {
                if (distance(port, end) <= NEAR_MISS_DISTANCE) {
                    reported.add(end);
                }
            }
        }
        reported.forEach(end -> warnings.add(Diagnostics.geometryMismatch(end)));
    }

    private static double distance(Point a, Point b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }
}
