package entity.design;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A circuit described by its logic only, with no positions or wires: the one format the app and
 * the AI use to talk about circuits (see {@code docs/circuit-json.md}).
 *
 * <p>Checked on construction: the limits below, names valid and unique ignoring case, every gate
 * input refers to a declared input, a gate or a constant, and every output is connected exactly
 * once. Not checked here (the builder's validator does that): feedback loops, and whether each
 * output depends on an input.
 *
 * @param name        the circuit's name, for display
 * @param inputs      input labels, in order
 * @param outputs     output labels, in order
 * @param gates       the gates, in order
 * @param connections for each output label, the input label, gate id or constant that drives it;
 *                    kept in the order of {@code outputs}
 */
public record CircuitDesign(String name, List<String> inputs, List<String> outputs, List<GateSpec> gates,
                            Map<String, String> connections) {

    public static final int MAX_INPUTS = 10;
    public static final int MAX_OUTPUTS = 8;
    public static final int MAX_GATES = 50;
    public static final int MAX_NAME_LENGTH = 60;

    public CircuitDesign {
        name = requireName(name);
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
        outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
        gates = List.copyOf(Objects.requireNonNull(gates, "gates"));
        Objects.requireNonNull(connections, "connections");
        requireCount("inputs", inputs.size(), 1, MAX_INPUTS);
        requireCount("outputs", outputs.size(), 1, MAX_OUTPUTS);
        requireCount("gates", gates.size(), 0, MAX_GATES);
        Map<String, String> kindByName = requireUniqueNames(inputs, outputs, gates);
        requireGateInputsDeclared(gates, kindByName);
        connections = orderedConnections(outputs, connections, kindByName);
    }

    /** @return the gate with this id, if there is one */
    public Optional<GateSpec> gate(String id) {
        return gates.stream().filter(gate -> gate.id().equals(id)).findFirst();
    }

    private static String requireName(String name) {
        String stripped = Objects.requireNonNull(name, "name").strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException("the circuit needs a name");
        }
        if (stripped.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("the circuit name is longer than " + MAX_NAME_LENGTH + " characters");
        }
        return stripped;
    }

    private static void requireCount(String what, int count, int min, int max) {
        if (count < min || count > max) {
            throw new IllegalArgumentException("a circuit needs " + min + " to " + max + " " + what + ", not " + count);
        }
    }

    /** @return each name, exactly as spelled, mapped to what it is: "input", "output" or "gate" */
    private static Map<String, String> requireUniqueNames(List<String> inputs, List<String> outputs,
                                                          List<GateSpec> gates) {
        Map<String, String> kindByName = new HashMap<>();
        Map<String, String> spellingByKey = new HashMap<>();
        for (String input : inputs) {
            addName(input, "input", kindByName, spellingByKey);
        }
        for (String output : outputs) {
            addName(output, "output", kindByName, spellingByKey);
        }
        for (GateSpec gate : gates) {
            addName(gate.id(), "gate", kindByName, spellingByKey);
        }
        return kindByName;
    }

    private static void addName(String name, String kind, Map<String, String> kindByName,
                                Map<String, String> spellingByKey) {
        if (!Signals.isValidName(name)) {
            throw new IllegalArgumentException(kind + " label \"" + name + "\" is not a valid name (a letter, then "
                    + "letters, digits or _, at most " + Signals.MAX_NAME_LENGTH + " characters)");
        }
        String previous = spellingByKey.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        if (previous != null) {
            throw new IllegalArgumentException("the name " + name + " is used twice (as " + kindByName.get(previous)
                    + " " + previous + " and as " + kind + " " + name + "); names must differ ignoring case");
        }
        kindByName.put(name, kind);
    }

    private static void requireGateInputsDeclared(List<GateSpec> gates, Map<String, String> kindByName) {
        for (GateSpec gate : gates) {
            for (String reference : gate.inputs()) {
                String signal = Signals.signalOf(reference);
                if (Signals.isConstant(signal)) {
                    continue;
                }
                String kind = kindOf(signal, kindByName);
                if (kind == null) {
                    throw new IllegalArgumentException("gate " + gate.id() + " uses " + signal
                            + ", which is not a declared input or gate");
                }
                if (kind.equals("output")) {
                    throw new IllegalArgumentException("gate " + gate.id() + " uses output " + signal
                            + " as an input; use the input or gate that drives " + signal + " instead");
                }
            }
        }
    }

    private static Map<String, String> orderedConnections(List<String> outputs, Map<String, String> connections,
                                                          Map<String, String> kindByName) {
        List<String> extra = new ArrayList<>(connections.keySet());
        extra.removeAll(outputs);
        if (!extra.isEmpty()) {
            throw new IllegalArgumentException("connections names " + String.join(", ", extra)
                    + ", which is not a declared output");
        }
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String output : outputs) {
            String driver = connections.get(output);
            if (driver == null) {
                throw new IllegalArgumentException("output " + output + " is not connected to anything");
            }
            requireDriver(output, driver, kindByName);
            ordered.put(output, driver);
        }
        return Collections.unmodifiableMap(ordered);
    }

    private static void requireDriver(String output, String driver, Map<String, String> kindByName) {
        if (Signals.isConstant(driver)) {
            return;
        }
        if (Signals.isNegated(driver)) {
            throw new IllegalArgumentException("output " + output + " is connected to " + driver
                    + "; outputs cannot be negated, add a NOT gate instead");
        }
        String kind = kindOf(driver, kindByName);
        if (kind == null || kind.equals("output")) {
            throw new IllegalArgumentException("output " + output + " is connected to " + driver
                    + ", which is not a declared input or gate");
        }
    }

    /** Names must match exactly; a match ignoring case only is reported as undeclared. */
    private static String kindOf(String signal, Map<String, String> kindByName) {
        return kindByName.get(signal);
    }
}
