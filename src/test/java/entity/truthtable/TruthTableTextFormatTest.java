package entity.truthtable;

import entity.simulation.Logic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TruthTableTextFormatTest {

    private final TruthTableTextFormat format = new TruthTableTextFormat();

    private static final String HALF_ADDER = """
            # Half adder. Lines starting with # are comments; blank lines are ignored.
            inputs: A, B
            outputs: Sum, Cout
            0 0 | 0 0
            0 1 | 1 0
            1 0 | 1 0
            1 1 | 0 1
            """;

    @Test
    void halfAdderExampleParsesCorrectly() throws TruthTableFormatException {
        TruthTable table = format.parse(HALF_ADDER);

        assertEquals(List.of("A", "B"), table.inputLabels());
        assertEquals(List.of("Sum", "Cout"), table.outputLabels());
        assertEquals(4, table.rows().size());
        assertEquals(List.of(Logic.ZERO, Logic.ZERO), table.rows().get(0).inputs());
        assertEquals(List.of(Logic.ZERO, Logic.ZERO), table.rows().get(0).outputs());
        assertEquals(List.of(Logic.ONE, Logic.ONE), table.rows().get(3).inputs());
        assertEquals(List.of(Logic.ZERO, Logic.ONE), table.rows().get(3).outputs());
    }

    @Test
    void acceptsCommentsBlankLinesCommasAndCrLf() throws TruthTableFormatException {
        String text = "# comment\r\n\r\ninputs: A,B\r\noutputs: Sum Cout\r\n0,0|0 0\r\n";
        TruthTable table = format.parse(text);

        assertEquals(List.of("A", "B"), table.inputLabels());
        assertEquals(List.of("Sum", "Cout"), table.outputLabels());
        assertEquals(1, table.rows().size());
    }

    @Test
    void xOutputsBecomeUnknown() throws TruthTableFormatException {
        TruthTable table = format.parse("""
                inputs: A
                outputs: Y
                0 | x
                1 | X
                """);

        assertEquals(List.of(Logic.UNKNOWN), table.rows().get(0).outputs());
        assertEquals(List.of(Logic.UNKNOWN), table.rows().get(1).outputs());
    }

    @Test
    void partialTableAllowed() throws TruthTableFormatException {
        TruthTable table = format.parse("""
                inputs: A, B
                outputs: Y
                0 1 | 1
                """);

        assertEquals(1, table.rows().size());
    }

    @Test
    void formatParseRoundTrip() throws TruthTableFormatException {
        TruthTable original = format.parse(HALF_ADDER);
        TruthTable roundTrip = format.parse(format.format(original));
        assertEquals(original, roundTrip);
    }

    @Test
    void formatUsesExpectedLayout() throws TruthTableFormatException {
        TruthTable table = format.parse("""
                inputs: A, B
                outputs: Sum, Cout
                0 1 | 1 0
                """);
        assertEquals("""
                inputs: A, B
                outputs: Sum, Cout
                0 1 | 1 0
                """, format.format(table));
    }

    @Test
    void blankTextHasNoRowsOrMissingInputs() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse(""));
        assertEquals(1, ex.line());
        assertEquals("line 1: missing 'inputs:' line", ex.getMessage());
    }

    @Test
    void tableWithHeadersButNoRows() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class,
                () -> format.parse("inputs: A\noutputs: Y\n"));
        assertEquals(3, ex.line());
        assertEquals("line 3: the table has no rows", ex.getMessage());
    }

    @Test
    void duplicateInputsLine() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: A
                inputs: B
                outputs: Y
                0 | 0
                """));
        assertEquals(2, ex.line());
        assertEquals("line 2: duplicate 'inputs:' line", ex.getMessage());
    }

    @Test
    void duplicateOutputsLine() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: A
                outputs: Y
                outputs: Z
                0 | 0
                """));
        assertEquals(3, ex.line());
        assertEquals("line 3: duplicate 'outputs:' line", ex.getMessage());
    }

    @Test
    void rowBeforeInputsLine() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                0 | 0
                inputs: A
                outputs: Y
                """));
        assertEquals(1, ex.line());
        assertEquals("line 1: missing 'inputs:' line", ex.getMessage());
    }

    @Test
    void rowBeforeOutputsLine() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: A
                0 | 0
                outputs: Y
                """));
        assertEquals(2, ex.line());
        assertEquals("line 2: missing 'outputs:' line", ex.getMessage());
    }

    @Test
    void invalidLabel() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: 1x
                outputs: Y
                0 | 0
                """));
        assertEquals(1, ex.line());
        assertEquals("line 1: invalid label \"1x\"", ex.getMessage());
    }

    @Test
    void duplicateLabelAcrossInputsAndOutputs() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: a
                outputs: A
                0 | 0
                """));
        assertEquals(2, ex.line());
        assertEquals("line 2: duplicate label \"A\"", ex.getMessage());
    }

    @Test
    void noInputLabels() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs:
                outputs: Y
                0 | 0
                """));
        assertEquals(1, ex.line());
        assertEquals("line 1: no input labels", ex.getMessage());
    }

    @Test
    void noOutputLabels() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: A
                outputs:
                0 | 0
                """));
        assertEquals(2, ex.line());
        assertEquals("line 2: no output labels", ex.getMessage());
    }

    @Test
    void missingBarBetweenInputsAndOutputs() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: A
                outputs: Y
                0 0
                """));
        assertEquals(3, ex.line());
        assertEquals("line 3: expected '|' between inputs and outputs", ex.getMessage());
    }

    @Test
    void wrongInputValueCount() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: A, B
                outputs: Y
                0 1 0 | 0
                """));
        assertEquals(3, ex.line());
        assertEquals("line 3: expected 2 input values, found 3", ex.getMessage());
    }

    @Test
    void wrongOutputValueCount() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: A, B
                outputs: Y, Z
                0 1 | 0
                """));
        assertEquals(3, ex.line());
        assertEquals("line 3: expected 2 output values, found 1", ex.getMessage());
    }

    @Test
    void invalidInputValue() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: A
                outputs: Y
                X | 0
                """));
        assertEquals(3, ex.line());
        assertEquals("line 3: invalid input value \"X\"", ex.getMessage());
    }

    @Test
    void invalidOutputValue() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: A
                outputs: Y
                0 | 2
                """));
        assertEquals(3, ex.line());
        assertEquals("line 3: invalid output value \"2\"", ex.getMessage());
    }

    @Test
    void duplicateRowForSameInputs() {
        TruthTableFormatException ex = assertThrows(TruthTableFormatException.class, () -> format.parse("""
                inputs: A, B
                outputs: Y
                0 1 | 1
                0 1 | 0
                """));
        assertEquals(4, ex.line());
        assertEquals("line 4: duplicate row for inputs 0 1", ex.getMessage());
    }
}
