package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.GateType;
import entity.design.CircuitDesign;
import entity.design.GateSpec;
import org.junit.jupiter.api.Test;
import use_case.build_circuit.DesignAssembler;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GateDepthCalculatorTest {
    private final GateDepthCalculator calculator = new GateDepthCalculator();
    private final DesignAssembler assembler = new DesignAssembler();

    @Test
    void calculatesDepthsWhenGatesAreListedInReverseOrder() {
        CircuitDesign design = designWithOutput("G2",
                new GateSpec("G2", GateType.NOT, List.of("G1")),
                new GateSpec("G1", GateType.NOT, List.of("A")));

        Map<String, Integer> depths = calculator.calculate(assembler.assemble(design));

        assertEquals(Map.of("G1", 1, "G2", 2), depths);
    }

    @Test
    void placesAGateAfterItsDeepestInput() {
        CircuitDesign design = designWithOutput("G3",
                new GateSpec("G1", GateType.NOT, List.of("A")),
                new GateSpec("G2", GateType.NOT, List.of("G1")),
                new GateSpec("G3", GateType.AND, List.of("A", "G2")));

        Map<String, Integer> depths = calculator.calculate(assembler.assemble(design));

        assertEquals(Map.of("G1", 1, "G2", 2, "G3", 3), depths);
    }

    @Test
    void allowsBranchesToShareAnUpstreamGate() {
        CircuitDesign design = designWithOutput("G4",
                new GateSpec("G1", GateType.NOT, List.of("A")),
                new GateSpec("G2", GateType.NOT, List.of("G1")),
                new GateSpec("G3", GateType.BUFFER, List.of("G1")),
                new GateSpec("G4", GateType.AND, List.of("G2", "G3")));

        Map<String, Integer> depths = calculator.calculate(assembler.assemble(design));

        assertEquals(Map.of("G1", 1, "G2", 2, "G3", 2, "G4", 3), depths);
    }

    @Test
    void treatsConstantsAsDepthZero() {
        CircuitDesign design = designWithOutput("G1",
                new GateSpec("G1", GateType.AND, List.of("A", "1")));

        Map<String, Integer> depths = calculator.calculate(assembler.assemble(design));

        assertEquals(Map.of("G1", 1), depths);
    }

    @Test
    void returnsAnEmptyMapForADirectConnection() {
        CircuitDesign design = designWithOutput("A");

        assertEquals(Map.of(), calculator.calculate(assembler.assemble(design)));
    }

    @Test
    void rejectsAFeedbackLoop() {
        CircuitDesign design = designWithOutput("G1",
                new GateSpec("G1", GateType.NOT, List.of("G2")),
                new GateSpec("G2", GateType.NOT, List.of("G1")));
        Circuit circuit = assembler.assemble(design);

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(circuit));
    }

    private static CircuitDesign designWithOutput(String driver, GateSpec... gates) {
        return new CircuitDesign("Depth test", List.of("A"), List.of("Y"),
                List.of(gates), Map.of("Y", driver));
    }
}
