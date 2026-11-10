package entity.simulation;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.GateType;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;
import entity.circuit.UnsupportedComponent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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

    /**
     * Orders gates so that every gate comes after the gates feeding it (Kahn's algorithm): start
     * with gates that wait on no other gate, and release a gate once everything it waits on is
     * placed. Gates that are never released feed each other, which is a feedback loop.
     */
    private static List<Gate> topologicalOrder(List<Gate> gates) throws NotSimulatableException {
        List<List<Integer>> feeds = gatesFedBy(gates);
        int[] waitingOn = new int[gates.size()];
        feeds.forEach(targets -> targets.forEach(target -> waitingOn[target]++));

        Queue<Integer> ready = new ArrayDeque<>();
        for (int i = 0; i < gates.size(); i++) {
            if (waitingOn[i] == 0) {
                ready.add(i);
            }
        }
        List<Gate> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            int gate = ready.remove();
            order.add(gates.get(gate));
            for (int target : feeds.get(gate)) {
                if (--waitingOn[target] == 0) {
                    ready.add(target);
                }
            }
        }
        if (order.size() != gates.size()) {
            throw new NotSimulatableException(
                    "The circuit has a feedback loop, so it is not combinational.");
        }
        return order;
    }

    /** @return for each gate (by index), the indexes of the gates that read its output */
    private static List<List<Integer>> gatesFedBy(List<Gate> gates) {
        Map<Integer, Integer> gateDrivingNet = new HashMap<>();
        List<List<Integer>> feeds = new ArrayList<>();
        for (int i = 0; i < gates.size(); i++) {
            gateDrivingNet.put(gates.get(i).outputNet(), i);
            feeds.add(new ArrayList<>());
        }
        for (int i = 0; i < gates.size(); i++) {
            for (int net : gates.get(i).inputNets()) {
                Integer source = gateDrivingNet.get(net);
                if (source != null) {
                    feeds.get(source).add(i);
                }
            }
        }
        return feeds;
    }

    private static Logic[] initialNets(Circuit circuit, Map<String, Logic> inputsByLabel) {
        Logic[] nets = new Logic[circuit.netCount()];
        Arrays.fill(nets, Logic.UNKNOWN);
        for (InputPin pin : circuit.inputPins()) {
            nets[pin.net()] = inputsByLabel.getOrDefault(pin.label(), Logic.UNKNOWN);
        }
        for (CircuitComponent component : circuit.components()) {
            if (component instanceof Constant constant) {
                nets[constant.net()] = Logic.of(constant.value());
            }
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
