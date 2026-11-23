package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.InputPin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Calculates gate depths for arranging a combinational circuit in columns. */
public final class GateDepthCalculator {

    /**
     * Calculates each gate's depth, regardless of its order in the circuit.
     *
     * @param circuit the circuit to arrange
     * @return depth keyed by gate ID
     * @throws IllegalArgumentException if the circuit has unsupported components,
     *         multiple drivers, a feedback loop, or an undriven gate input
     */
    public Map<String, Integer> calculate(Circuit circuit) {
        if (!circuit.isSimulatable()) {
            throw new IllegalArgumentException("Cannot lay out unsupported components.");
        }

        Map<Integer, Integer> depthByNet = sourceDepths(circuit);
        Map<String, Integer> depthByGate = new HashMap<>();
        List<Gate> remaining = new ArrayList<>(circuit.gates());

        while (!remaining.isEmpty()) {
            int previousSize = remaining.size();
            Iterator<Gate> iterator = remaining.iterator();
            while (iterator.hasNext()) {
                Gate gate = iterator.next();
                if (gate.inputNets().stream().allMatch(depthByNet::containsKey)) {
                    int depth = gate.inputNets().stream()
                            .mapToInt(depthByNet::get)
                            .max().orElseThrow() + 1;
                    depthByGate.put(gate.id(), depth);
                    depthByNet.put(gate.outputNet(), depth);
                    iterator.remove();
                }
            }

            if (remaining.size() == previousSize) {
                throw new IllegalArgumentException(
                        "Cannot determine gate depths: feedback loop or undriven gate input.");
            }
        }

        return Map.copyOf(depthByGate);
    }

    private Map<Integer, Integer> sourceDepths(Circuit circuit) {
        Map<Integer, Integer> depths = new HashMap<>();
        Set<Integer> drivenNets = new HashSet<>();

        for (CircuitComponent component : circuit.components()) {
            component.drivenNet().ifPresent(net -> {
                if (!drivenNets.add(net)) {
                    throw new IllegalArgumentException("Multiple components drive net " + net);
                }
                if (component instanceof InputPin || component instanceof Constant) {
                    depths.put(net, 0);
                }
            });
        }

        return depths;
    }
}
