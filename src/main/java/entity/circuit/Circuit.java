package entity.circuit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable, format-independent combinational circuit.
 *
 * <p>Invariants (checked on construction): component ids are unique, every net id is in
 * {@code [0, netCount)}, and pin labels are non-blank and unique ignoring case.
 *
 * @param name       the circuit's name
 * @param components all components, in source order
 * @param wires      wire segments, for drawing
 * @param netCount   number of nets; nets are numbered {@code 0..netCount-1}
 * @param warnings   non-fatal problems found while reading the circuit
 */
public record Circuit(String name, List<CircuitComponent> components, List<Wire> wires,
                      int netCount, List<CircuitWarning> warnings) {

    public Circuit {
        Objects.requireNonNull(name, "name");
        components = List.copyOf(components);
        wires = List.copyOf(wires);
        warnings = List.copyOf(warnings);
        if (netCount < 0) {
            throw new IllegalArgumentException("netCount must not be negative");
        }
        requireUniqueIds(components);
        requireNetsInRange(components, wires, netCount);
        requireUniquePinLabels(components);
    }

    /** @return input pins sorted by label in {@link LabelOrder#NATURAL} order */
    public List<InputPin> inputPins() {
        return components.stream()
                .filter(InputPin.class::isInstance)
                .map(InputPin.class::cast)
                .sorted((a, b) -> LabelOrder.NATURAL.compare(a.label(), b.label()))
                .toList();
    }

    /** @return output pins sorted by label in {@link LabelOrder#NATURAL} order */
    public List<OutputPin> outputPins() {
        return components.stream()
                .filter(OutputPin.class::isInstance)
                .map(OutputPin.class::cast)
                .sorted((a, b) -> LabelOrder.NATURAL.compare(a.label(), b.label()))
                .toList();
    }

    /** @return all gates, in source order */
    public List<Gate> gates() {
        return components.stream().filter(Gate.class::isInstance).map(Gate.class::cast).toList();
    }

    /** @return all components outside the supported subset, in source order */
    public List<UnsupportedComponent> unsupportedComponents() {
        return components.stream()
                .filter(UnsupportedComponent.class::isInstance)
                .map(UnsupportedComponent.class::cast)
                .toList();
    }

    /**
     * @return whether every component is supported. Feedback loops are not checked here; the
     *         simulator reports those.
     */
    public boolean isSimulatable() {
        return unsupportedComponents().isEmpty();
    }

    private static void requireUniqueIds(List<CircuitComponent> components) {
        Set<String> ids = new HashSet<>();
        for (CircuitComponent component : components) {
            if (!ids.add(component.id())) {
                throw new IllegalArgumentException("duplicate component id " + component.id());
            }
        }
    }

    private static void requireNetsInRange(List<CircuitComponent> components, List<Wire> wires,
                                           int netCount) {
        for (CircuitComponent component : components) {
            component.inputNets().forEach(net -> requireNet(net, netCount, component.id()));
            component.drivenNet().ifPresent(net -> requireNet(net, netCount, component.id()));
        }
        for (Wire wire : wires) {
            requireNet(wire.net(), netCount, "wire " + wire.from() + "-" + wire.to());
        }
    }

    private static void requireNet(int net, int netCount, String owner) {
        if (net < 0 || net >= netCount) {
            throw new IllegalArgumentException(owner + " uses net " + net + " outside [0," + netCount + ")");
        }
    }

    private static void requireUniquePinLabels(List<CircuitComponent> components) {
        Map<String, Pin> byLabel = new HashMap<>();
        for (CircuitComponent component : components) {
            if (component instanceof Pin pin) {
                if (pin.label().isBlank()) {
                    throw new IllegalArgumentException("pin " + pin.id() + " has a blank label");
                }
                Pin previous = byLabel.put(pin.label().toLowerCase(Locale.ROOT), pin);
                if (previous != null) {
                    throw new IllegalArgumentException(
                            "pins " + previous.id() + " and " + pin.id() + " share label " + pin.label());
                }
            }
        }
    }
}
