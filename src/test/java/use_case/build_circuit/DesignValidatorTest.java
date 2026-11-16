package use_case.build_circuit;

import entity.circuit.GateType;
import entity.design.CircuitDesign;
import entity.design.GateSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DesignValidatorTest {
    private final DesignValidator validator = new DesignValidator();

    @Test
    void acceptsAnAcyclicGateChain() {
        CircuitDesign design = designWithOutput("G2",
                new GateSpec("G1", GateType.NOT, List.of("A")),
                new GateSpec("G2", GateType.NOT, List.of("G1")));

        assertDoesNotThrow(() -> validator.validate(design));
    }

    @Test
    void rejectsAGateThatFeedsItself() {
        CircuitDesign design = designWithOutput("G1",
                new GateSpec("G1", GateType.NOT, List.of("G1")));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> validator.validate(design));

        assertEquals("Feedback loop detected at gate G1", error.getMessage());
    }

    @Test
    void rejectsALoopThroughANegatedReference() {
        CircuitDesign design = designWithOutput("G1",
                new GateSpec("G1", GateType.BUFFER, List.of("!G2")),
                new GateSpec("G2", GateType.BUFFER, List.of("G1")));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> validator.validate(design));

        assertEquals("Feedback loop detected at gate G1", error.getMessage());
    }

    @Test
    void acceptsBranchesThatShareAnUpstreamGate() {
        CircuitDesign design = designWithOutput("G4",
                new GateSpec("G1", GateType.NOT, List.of("A")),
                new GateSpec("G2", GateType.NOT, List.of("G1")),
                new GateSpec("G3", GateType.BUFFER, List.of("G1")),
                new GateSpec("G4", GateType.AND, List.of("G2", "G3")));

        assertDoesNotThrow(() -> validator.validate(design));
    }

    @Test
    void acceptsAnOutputDrivenDirectlyByAnInput() {
        CircuitDesign design = designWithOutput("A");

        assertDoesNotThrow(() -> validator.validate(design));
    }

    @Test
    void rejectsAnOutputDrivenDirectlyByAConstant() {
        CircuitDesign design = designWithOutput("1");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> validator.validate(design));

        assertEquals("Output Y does not depend on a circuit input", error.getMessage());
    }

    @Test
    void rejectsAnOutputWhoseGateOnlyReadsConstants() {
        CircuitDesign design = designWithOutput("G1",
                new GateSpec("G1", GateType.AND, List.of("0", "1")));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> validator.validate(design));

        assertEquals("Output Y does not depend on a circuit input", error.getMessage());
    }

    private CircuitDesign designWithOutput(String driver, GateSpec... gates) {
        return new CircuitDesign("Validator test", List.of("A"), List.of("Y"),
                List.of(gates), Map.of("Y", driver));
    }
}
