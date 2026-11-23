package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.GateType;
import entity.design.CircuitDesign;
import entity.design.GateSpec;
import entity.simulation.CircuitSimulator;
import entity.truthtable.TruthTableGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.build_circuit.DesignAssembler;
import use_case.save_built_circuit.CircuitFileWriteException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogisimCircuitWriterTest {
    @TempDir
    Path directory;

    private final LogisimCircuitWriter writer = createWriter();

    @Test
    void savesAHalfAdderThatLoadsWithTheSameTruthTable() throws Exception {
        Circuit original = halfAdder();
        Path target = directory.resolve("half_adder.circ");

        writer.write(original, target.toString());

        Circuit restored = new LogisimCircuitParser().load(target.toString());
        TruthTableGenerator tables = new TruthTableGenerator(new CircuitSimulator());

        assertTrue(Files.isRegularFile(target));
        assertEquals(tables.generate(original), tables.generate(restored));
        assertTrue(restored.warnings().isEmpty());
        assertOnlyTargetRemains(target);
    }

    @Test
    void replacesAnExistingFile() throws Exception {
        Path target = directory.resolve("existing.circ");
        Files.writeString(target, "old contents");

        writer.write(halfAdder(), target.toString());

        Circuit restored = new LogisimCircuitParser().load(target.toString());
        assertEquals("Half adder", restored.name());
        assertOnlyTargetRemains(target);
    }

    @Test
    void reportsFailureWhenTheDestinationFolderDoesNotExist() {
        Path target = directory.resolve("missing").resolve("circuit.circ");

        assertThrows(CircuitFileWriteException.class,
                () -> writer.write(halfAdder(), target.toString()));

        assertFalse(Files.exists(target));
    }

    @Test
    void rejectsABlankDestination() {
        assertThrows(CircuitFileWriteException.class,
                () -> writer.write(halfAdder(), "   "));
    }

    private void assertOnlyTargetRemains(Path target) throws Exception {
        try (var files = Files.list(directory)) {
            assertEquals(List.of(target), files.toList());
        }
    }

    private static LogisimCircuitWriter createWriter() {
        GatePortLocator ports = new GatePortLocator();
        CircuitWireRouter router = new CircuitWireRouter(
                new CircuitPortCollector(ports),
                new WirePathFinder(new WireRoutingRules(), new GateObstacleChecker(ports)));

        return new LogisimCircuitWriter(
                new CircuitLayout(new GateDepthCalculator()), router, new CircuitXmlEncoder());
    }

    private static Circuit halfAdder() {
        CircuitDesign design = new CircuitDesign("Half adder", List.of("A", "B"),
                List.of("Sum", "Cout"),
                List.of(
                        new GateSpec("G1", GateType.XOR, List.of("A", "B")),
                        new GateSpec("G2", GateType.AND, List.of("A", "B"))),
                Map.of("Sum", "G1", "Cout", "G2"));

        return new DesignAssembler().assemble(design);
    }
}
