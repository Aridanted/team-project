package use_case.build_circuit;

import entity.expression.ExpressionParser;
import entity.simulation.Logic;
import entity.truthtable.TruthTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputExpressionVerifierTest {
    private final OutputExpressionVerifier verifier = new OutputExpressionVerifier(new ExpressionParser());

    private final TruthTable inverter = new TruthTable(List.of("A"), List.of("Y"), List.of(
            new TruthTable.Row(List.of(Logic.ZERO), List.of(Logic.ONE)),
            new TruthTable.Row(List.of(Logic.ONE), List.of(Logic.ZERO))));

    @Test
    void acceptsAMatchingExpression() {
        assertDoesNotThrow(() -> verifier.verify(Map.of("Y", "!A"), inverter));
    }

    @Test
    void acceptsAnEmptyExpressionMap() {
        assertDoesNotThrow(() -> verifier.verify(Map.of(), inverter));
    }

    @Test
    void reportsTheFirstMismatchedRow() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> verifier.verify(Map.of("Y", "A"), inverter));

        assertTrue(error.getMessage().contains("output Y"));
        assertTrue(error.getMessage().contains("Row 1"));
    }

    @Test
    void rejectsAnUnknownOutputLabel() {
        assertThrows(IllegalArgumentException.class,
                () -> verifier.verify(Map.of("Z", "!A"), inverter));
    }

    @Test
    void rejectsAnUnknownInputVariable() {
        assertThrows(IllegalArgumentException.class,
                () -> verifier.verify(Map.of("Y", "!B"), inverter));
    }

    @Test
    void reportsThePositionOfInvalidSyntax() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> verifier.verify(Map.of("Y", "A &"), inverter));

        assertTrue(error.getMessage().contains("position 3"));
    }
}
