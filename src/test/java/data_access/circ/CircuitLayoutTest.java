package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.GateType;
import entity.circuit.Point;
import entity.design.CircuitDesign;
import entity.design.GateSpec;
import entity.simulation.CircuitSimulator;
import entity.simulation.NotSimulatableException;
import entity.truthtable.TruthTableGenerator;
import org.junit.jupiter.api.Test;
import use_case.build_circuit.DesignAssembler;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitLayoutTest {
    private final CircuitLayout layout = new CircuitLayout(new GateDepthCalculator());
    private final DesignAssembler assembler = new DesignAssembler();

    @Test
    void placesInputsGatesAndOutputsInDepthOrder() {
        CircuitDesign design = designWithOutput("G2",
                new GateSpec("G2", GateType.NOT, List.of("G1")),
                new GateSpec("G1", GateType.NOT, List.of("A")));

        Circuit positioned = layout.layout(assembler.assemble(design));

        assertEquals(100, positioned.inputPins().getFirst().location().x());
        assertEquals(340, gate(positioned, "G1").location().x());
        assertEquals(580, gate(positioned, "G2").location().x());
        assertEquals(820, positioned.outputPins().getFirst().location().x());
    }

    @Test
    void placesEveryComponentOnTheTenPixelGrid() {
        CircuitDesign design = designWithOutput("G1",
                new GateSpec("G1", GateType.AND, List.of("A", "1")));

        Circuit positioned = layout.layout(assembler.assemble(design));

        for (CircuitComponent component : positioned.components()) {
            assertEquals(0, component.location().x() % 10);
            assertEquals(0, component.location().y() % 10);
        }
    }

    @Test
    void separatesGatesInTheSameColumn() {
        CircuitDesign design = designWithOutput("G1",
                new GateSpec("G1", GateType.NOT, List.of("A")),
                new GateSpec("G2", GateType.BUFFER, List.of("A")));

        Circuit positioned = layout.layout(assembler.assemble(design));
        Point first = gate(positioned, "G1").location();
        Point second = gate(positioned, "G2").location();

        assertEquals(first.x(), second.x());
        assertTrue(Math.abs(first.y() - second.y()) >= 400);
    }

    @Test
    void placesConstantsInTheSourceColumn() {
        CircuitDesign design = designWithOutput("G1",
                new GateSpec("G1", GateType.AND, List.of("A", "1")));

        Circuit positioned = layout.layout(assembler.assemble(design));
        Constant constant = positioned.components().stream()
                .filter(Constant.class::isInstance)
                .map(Constant.class::cast)
                .findFirst().orElseThrow();

        assertEquals(positioned.inputPins().getFirst().location().x(), constant.location().x());
    }

    @Test
    void preservesTheSimulatedTruthTable() throws NotSimulatableException {
        CircuitDesign design = designWithOutput("G1",
                new GateSpec("G1", GateType.AND, List.of("!A", "1")));
        Circuit original = assembler.assemble(design);
        TruthTableGenerator generator = new TruthTableGenerator(new CircuitSimulator());

        Circuit positioned = layout.layout(original);

        assertEquals(generator.generate(original), generator.generate(positioned));
    }

    private static Gate gate(Circuit circuit, String id) {
        return circuit.gates().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst().orElseThrow();
    }

    private static CircuitDesign designWithOutput(String driver, GateSpec... gates) {
        return new CircuitDesign("Layout test", List.of("A"), List.of("Y"),
                List.of(gates), Map.of("Y", driver));
    }

    @Test
    void convertsNotWithNegatedInputIntoBuffer() throws NotSimulatableException {
        CircuitDesign design = designWithOutput("G1",
                new GateSpec("G1", GateType.NOT, List.of("!A")));
        Circuit original = assembler.assemble(design);

        Circuit positioned = layout.layout(original);
        Gate normalized = gate(positioned, "G1");

        assertEquals(GateType.BUFFER, normalized.type());
        assertEquals(false, normalized.inputs().getFirst().negated());

        TruthTableGenerator generator = new TruthTableGenerator(new CircuitSimulator());
        assertEquals(generator.generate(original), generator.generate(positioned));
    }

    @Test
    void convertsBufferWithNegatedInputIntoNot() throws NotSimulatableException {
        CircuitDesign design = designWithOutput("G1",
                new GateSpec("G1", GateType.BUFFER, List.of("!A")));
        Circuit original = assembler.assemble(design);

        Circuit positioned = layout.layout(original);
        Gate normalized = gate(positioned, "G1");

        assertEquals(GateType.NOT, normalized.type());
        assertEquals(false, normalized.inputs().getFirst().negated());

        TruthTableGenerator generator = new TruthTableGenerator(new CircuitSimulator());
        assertEquals(generator.generate(original), generator.generate(positioned));
    }
}
