package data_access.circ;

import entity.circuit.CircuitComponent;
import entity.circuit.CircuitWarning;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;
import entity.circuit.Pin;
import entity.circuit.Point;
import use_case.import_circuit.CircuitParseException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Makes pin labels usable as truth-table column names: rejects duplicates (ignoring case) and
 * names unlabelled pins IN1, IN2, ... and OUT1, OUT2, ... in reading order (top to bottom, then
 * left to right), skipping names already taken.
 */
final class PinLabeler {

    private static final Comparator<Pin> READING_ORDER = Comparator
            .comparingInt((Pin pin) -> pin.location().y())
            .thenComparingInt(pin -> pin.location().x());

    List<CircuitComponent> assignLabels(List<CircuitComponent> components, List<CircuitWarning> warnings)
            throws CircuitParseException {
        List<Pin> pins = components.stream().filter(Pin.class::isInstance).map(Pin.class::cast).toList();
        Map<String, Pin> taken = new HashMap<>();
        for (Pin pin : pins) {
            if (!pin.label().isEmpty()) {
                Pin previous = taken.put(key(pin.label()), pin);
                if (previous != null) {
                    throw Diagnostics.duplicateLabel(pin.label(), previous.location(), pin.location());
                }
            }
        }
        Map<String, String> assigned = new HashMap<>();
        nameUnlabelled(pins, InputPin.class, "IN", taken, assigned, warnings);
        nameUnlabelled(pins, OutputPin.class, "OUT", taken, assigned, warnings);
        return components.stream()
                .map(c -> c instanceof Pin pin && assigned.containsKey(pin.id())
                        ? pin.withLabel(assigned.get(pin.id()))
                        : c)
                .toList();
    }

    private static void nameUnlabelled(List<Pin> pins, Class<? extends Pin> kind, String prefix,
                                       Map<String, Pin> taken, Map<String, String> assigned,
                                       List<CircuitWarning> warnings) {
        List<Pin> unlabelled = pins.stream()
                .filter(kind::isInstance)
                .filter(pin -> pin.label().isEmpty())
                .sorted(READING_ORDER)
                .toList();
        int next = 1;
        for (Pin pin : unlabelled) {
            while (taken.containsKey(key(prefix + next))) {
                next++;
            }
            String label = prefix + next;
            taken.put(key(label), pin);
            assigned.put(pin.id(), label);
            Point at = pin.location();
            warnings.add(Diagnostics.unlabelledPin(at, label));
        }
    }

    private static String key(String label) {
        return label.toLowerCase(Locale.ROOT);
    }
}
