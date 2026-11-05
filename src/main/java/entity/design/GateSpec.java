package entity.design;

import entity.circuit.GateType;

import java.util.List;
import java.util.Objects;

/**
 * One gate of a {@link CircuitDesign}.
 *
 * @param id     the gate's name, unique in the design; other gates and outputs refer to it
 * @param type   the logic function
 * @param inputs signal references in port order: an input label, a gate id, {@code 0} or
 *               {@code 1}, each optionally prefixed with {@code !} to negate it. Whether each
 *               referenced name exists is checked by {@link CircuitDesign}.
 */
public record GateSpec(String id, GateType type, List<String> inputs) {

    /** Most inputs a multi-input gate may have (Logisim-evolution's own limit is higher). */
    public static final int MAX_INPUTS = 32;

    public GateSpec {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(inputs, "inputs");
        if (!Signals.isValidName(id)) {
            throw new IllegalArgumentException("gate id \"" + id + "\" is not a valid name "
                    + "(a letter, then letters, digits or _, at most " + Signals.MAX_NAME_LENGTH + " characters)");
        }
        inputs = List.copyOf(inputs);
        requireInputCount(id, type, inputs.size());
        for (String reference : inputs) {
            requireWellFormed(id, reference);
        }
    }

    private static void requireInputCount(String id, GateType type, int count) {
        if (type.isUnary() && count != 1) {
            throw new IllegalArgumentException("gate " + id + " is a " + type + " gate and needs exactly 1 input, not "
                    + count);
        }
        if (!type.isUnary() && (count < 2 || count > MAX_INPUTS)) {
            throw new IllegalArgumentException("gate " + id + " is a " + type + " gate and needs 2 to " + MAX_INPUTS
                    + " inputs, not " + count);
        }
    }

    private static void requireWellFormed(String id, String reference) {
        String signal = reference == null ? "" : Signals.signalOf(reference);
        if (!Signals.isConstant(signal) && !Signals.isValidName(signal)) {
            throw new IllegalArgumentException("gate " + id + " has input \"" + reference
                    + "\", which is not a signal name, 0 or 1");
        }
    }
}
