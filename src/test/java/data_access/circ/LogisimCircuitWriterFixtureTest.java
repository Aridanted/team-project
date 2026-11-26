package data_access.circ;

import entity.circuit.Circuit;
import entity.simulation.CircuitSimulator;
import entity.truthtable.TruthTableGenerator;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogisimCircuitWriterFixtureTest {
    @TempDir
    Path directory;

    private final LogisimCircuitWriter writer = createWriter();

    /**
     * Exports each supported example circuit and reloads it to verify that its
     * truth table is preserved and no parser warnings are produced.
     *
     * @param name the example circuit's file name without the extension
     * @throws Exception if loading, exporting, or simulating the circuit fails
     */
    @ParameterizedTest(name = "round trip: {0}")
    @ValueSource(strings = {
            "half_adder",
            "full_adder",
            "mux2",
            "xor_from_nand",
            "gate_sizes",
            "rotated",
            "negated_inputs",
            "constants"
    })
    void roundTripsTheEightExampleCircuits(String name) throws Exception {
        Circuit original = LogisimCircuitParserTest.parseFixture(name);
        Path target = directory.resolve(name + "_export.circ");

        writer.write(original, target.toString());

        Circuit restored = new LogisimCircuitParser().load(target.toString());
        TruthTableGenerator tables = new TruthTableGenerator(new CircuitSimulator());

        assertEquals(tables.generate(original), tables.generate(restored));
        assertTrue(restored.warnings().isEmpty(),
                () -> name + " produced warnings: " + restored.warnings());
    }

    private static LogisimCircuitWriter createWriter() {
        GatePortLocator ports = new GatePortLocator();
        CircuitWireRouter router = new CircuitWireRouter(
                new CircuitPortCollector(ports),
                new WirePathFinder(new WireRoutingRules(), new GateObstacleChecker(ports)));

        return new LogisimCircuitWriter(
                new CircuitLayout(new GateDepthCalculator()), router, new CircuitXmlEncoder());
    }
}
