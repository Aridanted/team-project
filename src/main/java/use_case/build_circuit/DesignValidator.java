package use_case.build_circuit;

import entity.design.CircuitDesign;
import entity.design.GateSpec;
import entity.design.Signals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Validates feedback loops and output dependencies in a circuit design. */
public class DesignValidator {

    /**
     * Checks that the design has no feedback loops and every output traces back to an input.
     *
     * @param design the circuit design to validate
     * @throws IllegalArgumentException if the design has a loop or an output with no path to an input
     */
    public void validate(CircuitDesign design) {
        Map<String, GateSpec> gatesById = new HashMap<>();
        for (GateSpec gate : design.gates()) {
            gatesById.put(gate.id(), gate);
        }

        Set<String> visiting = new HashSet<>();
        Set<String> checked = new HashSet<>();
        for (GateSpec gate : design.gates()) {
            checkGate(gate, gatesById, visiting, checked);
        }

        Set<String> inputIds = new HashSet<>(design.inputs());
        Map<String, Boolean> inputDependencyByGate = new HashMap<>();
        for (String output : design.outputs()) {
            String driver = design.connections().get(output);
            if (!dependsOnInput(driver, inputIds, gatesById, inputDependencyByGate)) {
                throw new IllegalArgumentException(
                        "Output " + output + " does not depend on a circuit input");
            }
        }
    }

    private void checkGate(GateSpec gate, Map<String, GateSpec> gatesById,
                           Set<String> visiting, Set<String> checked) {
        String gateId = gate.id();
        if (checked.contains(gateId)) {
            return;
        }
        if (!visiting.add(gateId)) {
            throw new IllegalArgumentException("Feedback loop detected at gate " + gateId);
        }

        for (String reference : gate.inputs()) {
            GateSpec upstreamGate = gatesById.get(Signals.signalOf(reference));
            if (upstreamGate != null) {
                checkGate(upstreamGate, gatesById, visiting, checked);
            }
        }

        visiting.remove(gateId);
        checked.add(gateId);
    }

    private boolean dependsOnInput(String reference, Set<String> inputIds,
                                   Map<String, GateSpec> gatesById,
                                   Map<String, Boolean> inputDependencyByGate) {
        String signal = Signals.signalOf(reference);
        if (inputIds.contains(signal)) {
            return true;
        }

        GateSpec gate = gatesById.get(signal);
        if (gate == null) {
            return false;
        }

        Boolean known = inputDependencyByGate.get(signal);
        if (known != null) {
            return known;
        }

        boolean reachesInput = gate.inputs().stream()
                .anyMatch(input -> dependsOnInput(input, inputIds, gatesById, inputDependencyByGate));
        inputDependencyByGate.put(signal, reachesInput);
        return reachesInput;
    }
}
