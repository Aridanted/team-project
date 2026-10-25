package entity.circuit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds {@link Circuit}s by hand for tests, so tests never need to parse .circ files. Signals are
 * named; every input, constant and gate defines one signal, and gate inputs and outputs refer to
 * signals by name. Prefix an input with {@code !} to negate it. A signal that is used but never
 * defined becomes a floating net, and a gate may use a signal defined later (for feedback loops).
 *
 * <pre>
 * Circuit c = CircuitTestFactory.builder("half adder")
 *         .input("A").input("B")
 *         .gate("s", GateType.XOR, "A", "B")
 *         .gate("c", GateType.AND, "A", "B")
 *         .output("Sum", "s").output("Cout", "c")
 *         .build();
 * </pre>
 */
public final class CircuitTestFactory {

    private CircuitTestFactory() {
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** Inputs A, B; outputs Sum = A xor B, Cout = A and B. */
    public static Circuit halfAdder() {
        return builder("half adder")
                .input("A").input("B")
                .gate("s", GateType.XOR, "A", "B")
                .gate("c", GateType.AND, "A", "B")
                .output("Sum", "s").output("Cout", "c")
                .build();
    }

    /** Inputs A, B, Cin; outputs Sum = A xor B xor Cin, Cout = majority(A, B, Cin). */
    public static Circuit fullAdder() {
        return builder("full adder")
                .input("A").input("B").input("Cin")
                .gate("x1", GateType.XOR, "A", "B")
                .gate("sum", GateType.XOR, "x1", "Cin")
                .gate("a1", GateType.AND, "A", "B")
                .gate("a2", GateType.AND, "x1", "Cin")
                .gate("cout", GateType.OR, "a1", "a2")
                .output("Sum", "sum").output("Cout", "cout")
                .build();
    }

    public static final class Builder {
        private final String name;
        private final List<CircuitComponent> components = new ArrayList<>();
        private final Map<String, Integer> netBySignal = new HashMap<>();
        private final Set<String> defined = new HashSet<>();
        private final List<CircuitWarning> warnings = new ArrayList<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder input(String label) {
            components.add(new InputPin(nextId(), label, define(label), nextPoint(), Orientation.EAST));
            return this;
        }

        public Builder constant(String signal, boolean value) {
            components.add(new Constant(nextId(), value, define(signal), nextPoint(), Orientation.EAST));
            return this;
        }

        /** @param inputs signal names, each optionally prefixed with {@code !} for a negated input */
        public Builder gate(String signal, GateType type, String... inputs) {
            List<GateInput> gateInputs = new ArrayList<>();
            for (String input : inputs) {
                boolean negated = input.startsWith("!");
                gateInputs.add(new GateInput(net(negated ? input.substring(1) : input), negated));
            }
            components.add(new Gate(nextId(), type, gateInputs, define(signal), nextPoint(), Orientation.EAST));
            return this;
        }

        public Builder output(String label, String signal) {
            components.add(new OutputPin(nextId(), label, net(signal), nextPoint(), Orientation.WEST));
            return this;
        }

        public Builder unsupported(String componentName) {
            components.add(new UnsupportedComponent(nextId(), componentName, "", nextPoint(), Orientation.EAST));
            return this;
        }

        public Builder warning(String code, String message) {
            warnings.add(new CircuitWarning(code, message));
            return this;
        }

        public Circuit build() {
            return new Circuit(name, components, List.of(), netBySignal.size(), warnings);
        }

        private int define(String signal) {
            if (!defined.add(signal)) {
                throw new IllegalArgumentException("signal " + signal + " is defined twice");
            }
            return net(signal);
        }

        private int net(String signal) {
            return netBySignal.computeIfAbsent(signal, s -> netBySignal.size());
        }

        private String nextId() {
            return "c" + components.size();
        }

        private Point nextPoint() {
            return new Point(0, 10 * components.size());
        }
    }
}
