package entity.expression;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionParserTest {
    private final ExpressionParser parser = new ExpressionParser();

    @Test
    void parsesVariablesAndConstants() {
        assertTrue(parser.parse(" input_1 ").evaluate(Map.of("input_1", true)));
        assertTrue(parser.parse("1").evaluate(Map.of()));
        assertFalse(parser.parse("0").evaluate(Map.of()));
    }

    @Test
    void supportsBothNotOperators() {
        Map<String, Boolean> values = Map.of("A", true);
        assertFalse(parser.parse("!A").evaluate(values));
        assertFalse(parser.parse("~A").evaluate(values));
        assertTrue(parser.parse("!!A").evaluate(values));
    }

    @Test
    void appliesOperatorPrecedence() {
        assertFalse(parser.parse("!A & B").evaluate(Map.of("A", false, "B", false)));
        assertTrue(parser.parse("A ^ B & C").evaluate(Map.of("A", true, "B", true, "C", false)));
        assertTrue(parser.parse("A | B ^ C").evaluate(Map.of("A", true, "B", true, "C", true)));
    }

    @Test
    void parenthesesOverridePrecedence() {
        Map<String, Boolean> values = Map.of("A", true, "B", false, "C", false);
        assertTrue(parser.parse("A | B & C").evaluate(values));
        assertFalse(parser.parse("(A | B) & C").evaluate(values));
    }

    @Test
    void reportsUnexpectedCharacterPosition() {
        ExpressionParseException error = assertThrows(
                ExpressionParseException.class, () -> parser.parse("A @ B"));
        assertEquals("Unexpected character at position 2", error.getMessage());
    }

    @Test
    void rejectsIncompleteExpressions() {
        assertThrows(ExpressionParseException.class, () -> parser.parse(""));
        assertThrows(ExpressionParseException.class, () -> parser.parse("A &"));
        assertThrows(ExpressionParseException.class, () -> parser.parse("(A | B"));
    }

    @Test
    void rejectsMissingVariableDuringEvaluation() {
        BooleanExpression expression = parser.parse("A");
        assertThrows(IllegalArgumentException.class, () -> expression.evaluate(Map.of()));
    }
}
