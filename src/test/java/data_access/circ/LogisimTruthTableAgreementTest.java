package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;
import entity.simulation.CircuitSimulator;
import entity.simulation.Logic;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The strongest parser and simulator check: for each fixture, every row of the truth table
 * Logisim-evolution 3.7.2 itself produced ({@code <fixture>.table.txt}, from
 * {@code logisim -t table}) must match what the parsed circuit computes. It runs twice per
 * fixture: with the independent {@link ReferenceEvaluator} (isolates the parser) and with the
 * app's {@link CircuitSimulator}.
 */
class LogisimTruthTableAgreementTest {

    private static final List<String> FIXTURES = List.of("half_adder", "full_adder", "mux2",
            "xor_from_nand", "gate_sizes", "rotated", "negated_inputs", "constants");

    /** Computes output label to '0', '1' or 'X' for one row of inputs. */
    @FunctionalInterface
    private interface Evaluator {
        Map<String, Character> outputs(Circuit circuit, Map<String, Boolean> inputs) throws Exception;
    }

    private static final Evaluator REFERENCE = (circuit, inputs) -> {
        Map<String, Character> outputs = new HashMap<>();
        ReferenceEvaluator.evaluate(circuit, inputs)
                .forEach((label, value) -> outputs.put(label, Character.toUpperCase(value)));
        return outputs;
    };

    private static final Evaluator SIMULATOR = (circuit, inputs) -> {
        Map<String, Logic> logicInputs = new HashMap<>();
        inputs.forEach((label, value) -> logicInputs.put(label, Logic.of(value)));
        Map<String, Character> outputs = new HashMap<>();
        new CircuitSimulator().evaluate(circuit, logicInputs)
                .forEach((label, value) -> outputs.put(label, value.symbol()));
        return outputs;
    };

    static Stream<Arguments> cases() {
        return FIXTURES.stream().flatMap(fixture -> Stream.of(
                Arguments.of(fixture, "reference", REFERENCE),
                Arguments.of(fixture, "simulator", SIMULATOR)));
    }

    @ParameterizedTest(name = "{0} ({1})")
    @MethodSource("cases")
    void parsedCircuitMatchesLogisim(String fixture, String evaluatorName, Evaluator evaluator) throws Exception {
        Circuit circuit = LogisimCircuitParserTest.parseFixture(fixture);
        assertEquals(List.of(), circuit.warnings(), "a fully wired fixture must parse without warnings");

        List<String> lines = tableLines(fixture);
        List<String> header = Arrays.asList(lines.get(0).trim().split("\\s+"));
        List<String> inputLabels = circuit.inputPins().stream().map(InputPin::label).toList();
        List<String> outputLabels = circuit.outputPins().stream().map(OutputPin::label).toList();
        assertEquals(inputLabels.size() + outputLabels.size(), header.size(), "column count");
        assertEquals(1 << inputLabels.size(), lines.size() - 1, "row count");

        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.trim().split("\\s+");
            Map<String, Boolean> inputs = new HashMap<>();
            for (String label : inputLabels) {
                inputs.put(label, cells[header.indexOf(label)].equals("1"));
            }
            Map<String, Character> actual = evaluator.outputs(circuit, inputs);
            for (String label : outputLabels) {
                assertEquals(cells[header.indexOf(label)].charAt(0), actual.get(label),
                        () -> fixture + " (" + evaluatorName + "): output " + label + " for inputs " + inputs);
            }
        }
    }

    private static List<String> tableLines(String fixture) throws IOException {
        try (InputStream in = LogisimTruthTableAgreementTest.class
                .getResourceAsStream("/circuits/" + fixture + ".table.txt")) {
            List<String> lines = new ArrayList<>();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
            return lines;
        }
    }
}
