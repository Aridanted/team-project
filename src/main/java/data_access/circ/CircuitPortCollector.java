package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.Gate;
import entity.circuit.OutputPin;
import entity.circuit.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Collects each net's source port followed by its destination ports. */
public final class CircuitPortCollector {
    private final GatePortLocator gatePorts;

    /**
     * Creates a collector with the gate port locator.
     *
     * @param gatePorts the locator for positioned gate inputs
     */
    public CircuitPortCollector(GatePortLocator gatePorts) {
        this.gatePorts = Objects.requireNonNull(gatePorts, "gatePorts");
    }

    /**
     * Collects ports from a positioned circuit.
     *
     * @param circuit the positioned circuit with normalized unary inputs
     * @return ports keyed by net, with the source first in each list
     * @throws IllegalArgumentException if a net has multiple drivers or an input has no driver
     */
    public Map<Integer, List<Point>> collect(Circuit circuit) {
        Map<Integer, List<Point>> portsByNet = new HashMap<>();

        for (CircuitComponent component : circuit.components()) {
            component.drivenNet().ifPresent(net -> {
                List<Point> ports = new ArrayList<>();
                ports.add(component.location());
                if (portsByNet.putIfAbsent(net, ports) != null) {
                    throw new IllegalArgumentException("Multiple components drive net " + net);
                }
            });
        }

        for (CircuitComponent component : circuit.components()) {
            if (component instanceof Gate gate) {
                List<Point> inputs = gatePorts.inputPorts(gate);
                for (int index = 0; index < inputs.size(); index++) {
                    addDestination(portsByNet, gate.inputs().get(index).net(), inputs.get(index));
                }
            } else if (component instanceof OutputPin pin) {
                addDestination(portsByNet, pin.net(), pin.location());
            }
        }

        Map<Integer, List<Point>> result = new HashMap<>();
        portsByNet.forEach((net, ports) -> result.put(net, List.copyOf(ports)));
        return Map.copyOf(result);
    }

    private void addDestination(Map<Integer, List<Point>> portsByNet, int net, Point destination) {
        List<Point> ports = portsByNet.get(net);
        if (ports == null) {
            throw new IllegalArgumentException("No component drives net " + net);
        }
        ports.add(destination);
    }
}
