package entity.design;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;
import entity.circuit.Pin;
import entity.circuit.UnsupportedComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns a parsed {@link Circuit} into a {@link CircuitDesign}, for example so the tutor can show
 * the AI the student's circuit. Gates are named {@code g1}, {@code g2}, ... in position order
 * (left to right, then top to bottom), skipping any name a pin already uses.
 */
public final class CircuitDescriber {

    private static final String GATE_PREFIX = "g";

    /**
     * @throws NotDescribableException if the circuit has unsupported components, an unconnected
     *                                 or multiply-driven wire, labels that are not valid names,
     *                                 or exceeds the design limits
     */
    public CircuitDesign describe(Circuit circuit) throws NotDescribableException {
        requireSupported(circuit);
        try {
            return build(circuit);
        } catch (IllegalArgumentException e) {
            throw new NotDescribableException(e.getMessage());
        }
    }

    private static CircuitDesign build(Circuit circuit) throws NotDescribableException {
        List<Gate> gates = inPositionOrder(circuit.gates());
        Map<String, String> gateNames = nameGates(gates, circuit);
        Map<Integer, List<CircuitComponent>> drivers = driversByNet(circuit);

        List<GateSpec> specs = new ArrayList<>();
        for (Gate gate : gates) {
            List<String> inputs = new ArrayList<>();
            for (GateInput input : gate.inputs()) {
                String signal = signalOn(input.net(), drivers, gateNames,
                        "the " + gate.type() + " gate at " + gate.location() + " has an unconnected input");
                inputs.add(Signals.reference(signal, input.negated()));
            }
            specs.add(new GateSpec(gateNames.get(gate.id()), gate.type(), inputs));
        }

        Map<String, String> connections = new LinkedHashMap<>();
        for (OutputPin output : circuit.outputPins()) {
            connections.put(output.label(), signalOn(output.net(), drivers, gateNames,
                    "output " + output.label() + " is not connected to anything"));
        }

        return new CircuitDesign(circuit.name().isBlank() ? "circuit" : circuit.name(),
                circuit.inputPins().stream().map(InputPin::label).toList(),
                circuit.outputPins().stream().map(OutputPin::label).toList(),
                specs, connections);
    }

    private static void requireSupported(Circuit circuit) throws NotDescribableException {
        List<String> unsupported = circuit.unsupportedComponents().stream()
                .map(UnsupportedComponent::name).distinct().toList();
        if (!unsupported.isEmpty()) {
            throw new NotDescribableException("the circuit contains components LogiCoach does not support: "
                    + String.join(", ", unsupported));
        }
    }

    private static List<Gate> inPositionOrder(List<Gate> gates) {
        return gates.stream()
                .sorted(Comparator.comparingInt((Gate gate) -> gate.location().x())
                        .thenComparingInt(gate -> gate.location().y())
                        .thenComparing(Gate::id))
                .toList();
    }

    private static Map<String, String> nameGates(List<Gate> gates, Circuit circuit) {
        Set<String> taken = new HashSet<>();
        for (CircuitComponent component : circuit.components()) {
            if (component instanceof Pin pin) {
                taken.add(pin.label().toLowerCase(Locale.ROOT));
            }
        }
        Map<String, String> names = new HashMap<>();
        int number = 0;
        for (Gate gate : gates) {
            String name;
            do {
                number++;
                name = GATE_PREFIX + number;
            } while (taken.contains(name));
            names.put(gate.id(), name);
        }
        return names;
    }

    private static Map<Integer, List<CircuitComponent>> driversByNet(Circuit circuit) {
        Map<Integer, List<CircuitComponent>> drivers = new HashMap<>();
        for (CircuitComponent component : circuit.components()) {
            component.drivenNet().ifPresent(net -> drivers.computeIfAbsent(net, n -> new ArrayList<>()).add(component));
        }
        return drivers;
    }

    private static String signalOn(int net, Map<Integer, List<CircuitComponent>> drivers,
                                   Map<String, String> gateNames, String unconnectedMessage)
            throws NotDescribableException {
        List<CircuitComponent> sources = drivers.getOrDefault(net, List.of());
        if (sources.isEmpty()) {
            throw new NotDescribableException(unconnectedMessage);
        }
        if (sources.size() > 1) {
            throw new NotDescribableException("several components drive the same wire (at "
                    + sources.get(0).location() + " and " + sources.get(1).location() + ")");
        }
        return switch (sources.get(0)) {
            case InputPin pin -> pin.label();
            case Constant constant -> constant.value() ? Signals.ONE : Signals.ZERO;
            case Gate gate -> gateNames.get(gate.id());
            case OutputPin pin -> throw new IllegalStateException("output pins drive nothing: " + pin.id());
            case UnsupportedComponent unsupported -> throw new IllegalStateException(
                    "unsupported components were rejected: " + unsupported.id());
        };
    }
}
