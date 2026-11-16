package use_case.build_circuit;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.InputPin;
import entity.circuit.Orientation;
import entity.circuit.OutputPin;
import entity.circuit.Point;
import entity.design.CircuitDesign;
import entity.design.GateSpec;
import entity.design.Signals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts a validated circuit design into a simulatable circuit. */
public class DesignAssembler {
    private static final Point PLACEHOLDER_LOCATION = new Point(0, 0);
    private static final Orientation FACING = Orientation.EAST;

    /**
     * Assigns nets to a validated design and builds its circuit components.
     *
     * @param design a design that has passed validation
     * @return the assembled circuit
     */
    public Circuit assemble(CircuitDesign design) {
        Map<String, Integer> netsBySignal = new HashMap<>();
        List<CircuitComponent> components = new ArrayList<>();

        for (String input : design.inputs()) {
            int net = netsBySignal.size();
            netsBySignal.put(input, net);
            components.add(new InputPin(input, input, net, PLACEHOLDER_LOCATION, FACING));
        }

        // Reserve every gate's output net before connecting any gate inputs.
        for (GateSpec gate : design.gates()) {
            netsBySignal.put(gate.id(), netsBySignal.size());
        }

        for (GateSpec gate : design.gates()) {
            List<GateInput> inputs = new ArrayList<>();
            for (String reference : gate.inputs()) {
                int net = netFor(reference, netsBySignal, components);
                inputs.add(new GateInput(net, Signals.isNegated(reference)));
            }
            components.add(new Gate(gate.id(), gate.type(), inputs,
                    netsBySignal.get(gate.id()), PLACEHOLDER_LOCATION, FACING));
        }

        for (String output : design.outputs()) {
            int net = netFor(design.connections().get(output), netsBySignal, components);
            components.add(new OutputPin(output, output, net, PLACEHOLDER_LOCATION, FACING));
        }

        return new Circuit(design.name(), components, List.of(), netsBySignal.size(), List.of());
    }

    private int netFor(String reference, Map<String, Integer> netsBySignal,
                       List<CircuitComponent> components) {
        String signal = Signals.signalOf(reference);
        Integer existingNet = netsBySignal.get(signal);
        if (existingNet != null) {
            return existingNet;
        }

        if (!Signals.isConstant(signal)) {
            throw new IllegalArgumentException("Undeclared signal " + signal);
        }

        int net = netsBySignal.size();
        netsBySignal.put(signal, net);
        components.add(new Constant("constant:" + signal, Signals.ONE.equals(signal),
                net, PLACEHOLDER_LOCATION, FACING));
        return net;
    }
}
