package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal three-valued evaluator used only to check parser output against Logisim's own truth
 * tables. It is deliberately simple (repeat until stable) and is not the app's simulator.
 */
final class ReferenceEvaluator {

    private ReferenceEvaluator() {
    }

    /** @return output label to '0', '1' or 'x' (unknown) */
    static Map<String, Character> evaluate(Circuit circuit, Map<String, Boolean> inputs) {
        Boolean[] nets = new Boolean[circuit.netCount()];
        for (CircuitComponent component : circuit.components()) {
            if (component instanceof InputPin pin) {
                nets[pin.net()] = inputs.get(pin.label());
            } else if (component instanceof Constant constant) {
                nets[constant.net()] = constant.value();
            }
        }
        boolean changed = true;
        for (int round = 0; changed && round <= circuit.gates().size(); round++) {
            changed = false;
            for (Gate gate : circuit.gates()) {
                Boolean value = apply(gate, nets);
                if (value != null && !value.equals(nets[gate.outputNet()])) {
                    nets[gate.outputNet()] = value;
                    changed = true;
                }
            }
        }
        Map<String, Character> outputs = new HashMap<>();
        for (OutputPin pin : circuit.outputPins()) {
            Boolean value = nets[pin.net()];
            outputs.put(pin.label(), value == null ? 'x' : value ? '1' : '0');
        }
        return outputs;
    }

    private static Boolean apply(Gate gate, Boolean[] nets) {
        List<Boolean> in = new ArrayList<>();
        for (GateInput input : gate.inputs()) {
            Boolean value = nets[input.net()];
            in.add(value == null ? null : value ^ input.negated());
        }
        return switch (gate.type()) {
            case AND -> and(in);
            case OR -> or(in);
            case NAND -> not(and(in));
            case NOR -> not(or(in));
            case XOR -> parity(in);
            case XNOR -> not(parity(in));
            case XOR_ONE -> exactlyOne(in);
            case XNOR_ONE -> not(exactlyOne(in));
            case NOT -> not(in.get(0));
            case BUFFER -> in.get(0);
        };
    }

    private static Boolean and(List<Boolean> in) {
        if (in.contains(Boolean.FALSE)) {
            return false;
        }
        return in.contains(null) ? null : true;
    }

    private static Boolean or(List<Boolean> in) {
        if (in.contains(Boolean.TRUE)) {
            return true;
        }
        return in.contains(null) ? null : false;
    }

    private static Boolean parity(List<Boolean> in) {
        return in.contains(null) ? null : in.stream().filter(b -> b).count() % 2 == 1;
    }

    private static Boolean exactlyOne(List<Boolean> in) {
        return in.contains(null) ? null : in.stream().filter(b -> b).count() == 1;
    }

    private static Boolean not(Boolean value) {
        return value == null ? null : !value;
    }
}
