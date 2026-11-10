package entity.truthtable;

import entity.circuit.Circuit;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;
import entity.simulation.CircuitEvaluator;
import entity.simulation.Logic;
import entity.simulation.NotSimulatableException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Compares a circuit with an expected truth table, row by row. */
public final class TruthTableChecker {

    private final CircuitEvaluator evaluator;

    public TruthTableChecker(CircuitEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    /**
     * @throws LabelMismatchException  if the expected table's labels differ from the circuit's pins
     * @throws NotSimulatableException if the circuit cannot be simulated
     */
    public CheckResult check(Circuit circuit, TruthTable expected)
            throws LabelMismatchException, NotSimulatableException {
        List<String> circuitInputLabels = circuit.inputPins().stream().map(InputPin::label).toList();
        List<String> circuitOutputLabels = circuit.outputPins().stream().map(OutputPin::label).toList();

        validateLabelSet("inputs", expected.inputLabels(), circuitInputLabels);
        validateLabelSet("outputs", expected.outputLabels(), circuitOutputLabels);

        // The student may type labels in any case ("sum" for "Sum"), so translate to the circuit's.
        Map<String, String> inputLabelInCircuit = labelMap(expected.inputLabels(), circuitInputLabels);
        Map<String, String> outputLabelInCircuit = labelMap(expected.outputLabels(), circuitOutputLabels);

        List<CheckResult.RowResult> rowResults = new ArrayList<>();
        for (int i = 0; i < expected.rows().size(); i++) {
            rowResults.add(checkRow(circuit, expected, i, inputLabelInCircuit, outputLabelInCircuit));
        }
        return new CheckResult(expected, rowResults);
    }

    private CheckResult.RowResult checkRow(Circuit circuit, TruthTable expected, int index,
                                           Map<String, String> inputLabelInCircuit,
                                           Map<String, String> outputLabelInCircuit)
            throws NotSimulatableException {
        TruthTable.Row row = expected.rows().get(index);
        Map<String, Logic> inputs = new HashMap<>();
        for (int j = 0; j < expected.inputLabels().size(); j++) {
            inputs.put(inputLabelInCircuit.get(expected.inputLabels().get(j)), row.inputs().get(j));
        }
        Map<String, Logic> outputs = evaluator.evaluate(circuit, inputs);

        List<Logic> actual = new ArrayList<>();
        List<String> failingOutputs = new ArrayList<>();
        for (int j = 0; j < expected.outputLabels().size(); j++) {
            String label = expected.outputLabels().get(j);
            Logic expectedValue = row.outputs().get(j);
            Logic actualValue = outputs.get(outputLabelInCircuit.get(label));
            actual.add(actualValue);
            if (expectedValue.isKnown() && expectedValue != actualValue) {
                failingOutputs.add(label);
            }
        }
        return new CheckResult.RowResult(index, row.inputs(), row.outputs(), actual, failingOutputs);
    }

    private static void validateLabelSet(String kind, List<String> specLabels, List<String> circuitLabels)
            throws LabelMismatchException {
        Set<String> specSet = specLabels.stream()
                .map(label -> label.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<String> circuitSet = circuitLabels.stream()
                .map(label -> label.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!specSet.equals(circuitSet)) {
            throw new LabelMismatchException("Your table has " + kind + " {"
                    + String.join(", ", specLabels) + "} but the circuit has {"
                    + String.join(", ", circuitLabels) + "}.");
        }
    }

    private static Map<String, String> labelMap(List<String> specLabels, List<String> circuitLabels) {
        Map<String, String> byLowerCircuit = new HashMap<>();
        for (String circuitLabel : circuitLabels) {
            byLowerCircuit.put(circuitLabel.toLowerCase(Locale.ROOT), circuitLabel);
        }
        Map<String, String> specToCircuit = new HashMap<>();
        for (String specLabel : specLabels) {
            specToCircuit.put(specLabel, byLowerCircuit.get(specLabel.toLowerCase(Locale.ROOT)));
        }
        return specToCircuit;
    }
}
