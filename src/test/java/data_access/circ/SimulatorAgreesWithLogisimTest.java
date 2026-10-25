package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;
import entity.simulation.CircuitSimulator;
import entity.simulation.Logic;
import entity.simulation.NotSimulatableException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import use_case.import_circuit.CircuitParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Same check as {@link LogisimTruthTableAgreementTest}, but outputs come from
 * {@link CircuitSimulator} instead of {@link ReferenceEvaluator}.
 */
class SimulatorAgreesWithLogisimTest {

    private final CircuitSimulator simulator = new CircuitSimulator();

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"half_adder", "full_adder", "mux2", "xor_from_nand", "gate_sizes",
            "rotated", "negated_inputs", "constants"})
    void parsedCircuitMatchesLogisim(String fixture) throws CircuitParseException, IOException,
            NotSimulatableException {
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
            Map<String, Logic> inputs = new HashMap<>();
            for (String label : inputLabels) {
                inputs.put(label, Logic.of(cells[header.indexOf(label)].equals("1")));
            }
            Map<String, Logic> actual = simulator.evaluate(circuit, inputs);
            for (String label : outputLabels) {
                assertEquals(cells[header.indexOf(label)].charAt(0), actual.get(label).symbol(),
                        () -> fixture + ": output " + label + " for inputs " + inputs);
            }
        }
    }

    private static List<String> tableLines(String fixture) throws IOException {
        try (InputStream in = SimulatorAgreesWithLogisimTest.class
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
