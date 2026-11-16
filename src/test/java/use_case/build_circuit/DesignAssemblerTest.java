package use_case.build_circuit;

import entity.circuit.Circuit;
import entity.circuit.GateType;
import entity.design.CircuitDesign;
import entity.design.GateSpec;
import entity.simulation.CircuitSimulator;
import entity.simulation.Logic;
import entity.simulation.NotSimulatableException;
import entity.truthtable.TruthTable;
import entity.truthtable.TruthTableGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DesignAssemblerTest {
    private final DesignAssembler assembler = new DesignAssembler();
    private final TruthTableGenerator tableGenerator = new TruthTableGenerator(new CircuitSimulator());

    @Test
    void assemblesForwardReferenceWithNegatedInputAndConstant() throws NotSimulatableException {
        CircuitDesign design = new CircuitDesign("Inverter", List.of("A"), List.of("Y"),
                List.of(
                        new GateSpec("G2", GateType.BUFFER, List.of("G1")),
                        new GateSpec("G1", GateType.AND, List.of("!A", "1"))),
                Map.of("Y", "G2"));

        Circuit circuit = assembler.assemble(design);
        TruthTable table = tableGenerator.generate(circuit);

        assertEquals(List.of("A"), table.inputLabels());
        assertEquals(List.of("Y"), table.outputLabels());
        assertEquals(List.of(
                new TruthTable.Row(List.of(Logic.ZERO), List.of(Logic.ONE)),
                new TruthTable.Row(List.of(Logic.ONE), List.of(Logic.ZERO))), table.rows());
    }

    @Test
    void connectsAnOutputDirectlyToAnInput() {
        CircuitDesign design = new CircuitDesign("Pass through", List.of("A"), List.of("Y"),
                List.of(), Map.of("Y", "A"));

        Circuit circuit = assembler.assemble(design);

        assertEquals(1, circuit.netCount());
        assertEquals(circuit.inputPins().getFirst().net(), circuit.outputPins().getFirst().net());
    }
}
