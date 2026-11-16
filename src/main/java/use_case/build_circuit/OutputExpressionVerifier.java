package use_case.build_circuit;

import entity.expression.BooleanExpression;
import entity.expression.ExpressionParser;
import entity.simulation.Logic;
import entity.truthtable.TruthTable;

import java.util.HashMap;
import java.util.Map;

/** Checks generated Boolean expressions against simulated circuit outputs. */
public class OutputExpressionVerifier {
    private final ExpressionParser parser;

    /**
     * Creates a verifier with the parser used to read the supplied expressions.
     *
     * @param parser the Boolean expression parser
     */
    public OutputExpressionVerifier(ExpressionParser parser) {
        this.parser = parser;
    }

    /**
     * Checks every supplied output expression against every simulated row.
     *
     * @param outputExpressions expressions keyed by output label
     * @param table the truth table produced by the simulator
     * @throws IllegalArgumentException if a supplied expression cannot be verified
     */
    public void verify(Map<String, String> outputExpressions, TruthTable table) {
        for (Map.Entry<String, String> entry : outputExpressions.entrySet()) {
            String output = entry.getKey();
            try {
                verifyOutput(output, entry.getValue(), table);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(
                        "Expression for output " + output + " failed verification. " + error.getMessage(), error);
            }
        }
    }

    private void verifyOutput(String output, String source, TruthTable table) {
        int outputIndex = table.outputLabels().indexOf(output);
        if (outputIndex < 0) {
            throw new IllegalArgumentException("Unknown output label");
        }

        BooleanExpression expression = parser.parse(source);
        for (int rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
            TruthTable.Row row = table.rows().get(rowIndex);
            Logic claimed = Logic.of(expression.evaluate(inputValues(table, row)));
            Logic simulated = row.outputs().get(outputIndex);
            if (claimed != simulated) {
                throw new IllegalArgumentException("Row " + (rowIndex + 1)
                        + " gives " + claimed.symbol() + " but the simulator gives " + simulated.symbol());
            }
        }
    }

    private Map<String, Boolean> inputValues(TruthTable table, TruthTable.Row row) {
        Map<String, Boolean> values = new HashMap<>();
        for (int index = 0; index < table.inputLabels().size(); index++) {
            String label = table.inputLabels().get(index);
            Logic value = row.inputs().get(index);
            if (!value.isKnown()) {
                throw new IllegalArgumentException("Input " + label + " has an unknown value");
            }
            values.put(label, value == Logic.ONE);
        }
        return values;
    }
}
