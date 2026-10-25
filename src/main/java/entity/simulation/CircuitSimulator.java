package entity.simulation;

import entity.circuit.Circuit;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.GateType;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;
import entity.circuit.UnsupportedComponent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

/** Evaluates combinational circuits with three-valued logic. */
public final class CircuitSimulator implements CircuitEvaluator {

    private final Map<GateType, GateFunction> gateFunctions;

    /** Uses {@link GateFunctions#standard()}. */
    public CircuitSimulator() {
        this(GateFunctions.standard());
    }

    /**
     * @param gateFunctions logic function per gate type; allows custom behaviour without changing
     *                      this class
     */
    public CircuitSimulator(Map<GateType, GateFunction> gateFunctions) {
        this.gateFunctions = Map.copyOf(Objects.requireNonNull(gateFunctions, "gateFunctions"));
    }

    @Override
    public Map<String, Logic> evaluate(Circuit circuit, Map<String, Logic> inputsByLabel)
            throws NotSimulatableException {
        checkSimulatable(circuit);
        List<Gate> gates = circuit.gates();
        List<Gate> order = topologicalOrder(gates);
        Logic[] nets = initialNets(circuit, inputsByLabel);
        evaluateGates(order, nets);
        return outputValues(circuit, nets);
    }

    private static void checkSimulatable(Circuit circuit) throws NotSimulatableException {
        List<UnsupportedComponent> unsupported = circuit.unsupportedComponents();
        if (!unsupported.isEmpty()) {
            String names = unsupported.stream()
                    .map(UnsupportedComponent::name)
                    .collect(Collectors.joining(", "));
            throw new NotSimulatableException(
                    "The circuit contains unsupported components: " + names + ".");
        }
    }

    private List<Gate> topologicalOrder(List<Gate> gates) throws NotSimulatableException {
        Map<Integer, Integer> netToGateIndex = new LinkedHashMap<>();
        for (int i = 0; i < gates.size(); i++) {
            netToGateIndex.put(gates.get(i).outputNet(), i);
        }
        int[] inDegree = new int[gates.size()];
        List<List<Integer>> dependents = new ArrayList<>();
        for (int i = 0; i < gates.size(); i++) {
            dependents.add(new ArrayList<>());
        }
        for (int i = 0; i < gates.size(); i++) {
            for (int net : gates.get(i).inputNets()) {
                Integer source = netToGateIndex.get(net);
                if (source != null) {
                    inDegree[i]++;
                    dependents.get(source).add(i);
                }
            }
        }
        Queue<Integer> ready = new ArrayDeque<>();
        for (int i = 0; i < gates.size(); i++) {
            if (inDegree[i] == 0) {
                ready.add(i);
            }
        }
        List<Gate> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            int index = ready.remove();
            order.add(gates.get(index));
            for (int dependent : dependents.get(index)) {
                if (--inDegree[dependent] == 0) {
                    ready.add(dependent);
                }
            }
        }
        if (order.size() != gates.size()) {
            throw new NotSimulatableException(
                    "The circuit has a feedback loop, so it is not combinational.");
        }
        return order;
    }

    private Logic[] initialNets(Circuit circuit, Map<String, Logic> inputsByLabel) {
        Logic[] nets = new Logic[circuit.netCount()];
        for (int i = 0; i < nets.length; i++) {
            nets[i] = Logic.UNKNOWN;
        }
        for (InputPin pin : circuit.inputPins()) {
            Logic value = inputsByLabel.get(pin.label());
            nets[pin.net()] = value != null ? value : Logic.UNKNOWN;
        }
        for (Constant constant : circuit.components().stream()
                .filter(Constant.class::isInstance)
                .map(Constant.class::cast)
                .toList()) {
            nets[constant.net()] = Logic.of(constant.value());
        }
        return nets;
    }

    private void evaluateGates(List<Gate> order, Logic[] nets) {
        for (Gate gate : order) {
            GateFunction function = gateFunctions.get(gate.type());
            List<Logic> inputs = new ArrayList<>(gate.inputs().size());
            for (GateInput input : gate.inputs()) {
                Logic value = nets[input.net()];
                inputs.add(input.negated() ? value.not() : value);
            }
            nets[gate.outputNet()] = function.apply(inputs);
        }
    }

    private static Map<String, Logic> outputValues(Circuit circuit, Logic[] nets) {
        Map<String, Logic> outputs = new LinkedHashMap<>();
        for (OutputPin pin : circuit.outputPins()) {
            outputs.put(pin.label(), nets[pin.net()]);
        }
        return Collections.unmodifiableMap(outputs);
    }
}
