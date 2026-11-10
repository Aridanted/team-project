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
import java.util.Map;
import java.util.Objects;

/** Builds a circuit's complete truth table by evaluating every input combination. */
public final class TruthTableGenerator {

    /** Largest number of inputs enumerated (1024 rows). */
    public static final int MAX_INPUTS = 10;

    private final CircuitEvaluator evaluator;

    public TruthTableGenerator(CircuitEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    /**
     * @param circuit the circuit to enumerate
     * @return a truth table with rows in binary-count order (all-zero inputs first)
     * @throws NotSimulatableException if the circuit cannot be simulated or has too many inputs
     */
    public TruthTable generate(Circuit circuit) throws NotSimulatableException {
        List<String> inputLabels = circuit.inputPins().stream().map(InputPin::label).toList();
        List<String> outputLabels = circuit.outputPins().stream().map(OutputPin::label).toList();
        int inputCount = inputLabels.size();
        if (inputCount > MAX_INPUTS) {
            throw new NotSimulatableException(
                    "The circuit has " + inputCount + " inputs; at most 10 are supported.");
        }
        int rowCount = 1 << inputCount;
        List<TruthTable.Row> rows = new ArrayList<>(rowCount);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Map<String, Logic> inputsByLabel = new HashMap<>();
            List<Logic> inputValues = new ArrayList<>(inputCount);
            for (int i = 0; i < inputCount; i++) {
                boolean bit = ((rowIndex >> (inputCount - 1 - i)) & 1) == 1;
                Logic value = Logic.of(bit);
                inputValues.add(value);
                inputsByLabel.put(inputLabels.get(i), value);
            }
            Map<String, Logic> outputsByLabel = evaluator.evaluate(circuit, inputsByLabel);
            List<Logic> outputValues = new ArrayList<>(outputLabels.size());
            for (String label : outputLabels) {
                outputValues.add(outputsByLabel.get(label));
            }
            rows.add(new TruthTable.Row(inputValues, outputValues));
        }
        return new TruthTable(inputLabels, outputLabels, rows);
    }
}
