package entity.truthtable;

import entity.circuit.Circuit;
import entity.circuit.CircuitTestFactory;
import entity.simulation.CircuitEvaluator;
import entity.simulation.Logic;
import entity.simulation.NotSimulatableException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TruthTableCheckerTest {

    private static final CircuitEvaluator HALF_ADDER_EVALUATOR = (circuit, inputs) -> {
        Logic a = inputs.getOrDefault("A", Logic.UNKNOWN);
        Logic b = inputs.getOrDefault("B", Logic.UNKNOWN);
        Map<String, Logic> outputs = new HashMap<>();
        if (a.isKnown() && b.isKnown()) {
            boolean av = a == Logic.ONE;
            boolean bv = b == Logic.ONE;
            outputs.put("Sum", Logic.of(av ^ bv));
            outputs.put("Cout", Logic.of(av && bv));
        } else {
            outputs.put("Sum", Logic.UNKNOWN);
            outputs.put("Cout", Logic.UNKNOWN);
        }
        return outputs;
    };

    private final TruthTableTextFormat textFormat = new TruthTableTextFormat();

    @Test
    void correctSpecPassesAllRows() throws Exception {
        Circuit circuit = CircuitTestFactory.halfAdder();
        TruthTable expected = textFormat.parse("""
                inputs: A, B
                outputs: Sum, Cout
                0 0 | 0 0
                0 1 | 1 0
                1 0 | 1 0
                1 1 | 0 1
                """);
        TruthTableChecker checker = new TruthTableChecker(HALF_ADDER_EVALUATOR);

        CheckResult result = checker.check(circuit, expected);

        assertTrue(result.allPassed());
        assertEquals(4, result.passedRows());
        assertEquals(4, result.totalRows());
        assertEquals(List.of(), result.failingOutputs());
    }

    @Test
    void wrongCellFailsThatRowOnly() throws Exception {
        Circuit circuit = CircuitTestFactory.halfAdder();
        TruthTable expected = textFormat.parse("""
                inputs: A, B
                outputs: Sum, Cout
                0 0 | 0 0
                0 1 | 0 0
                1 0 | 1 0
                1 1 | 0 1
                """);
        TruthTableChecker checker = new TruthTableChecker(HALF_ADDER_EVALUATOR);

        CheckResult result = checker.check(circuit, expected);

        assertFalse(result.allPassed());
        assertEquals(3, result.passedRows());
        assertEquals(List.of("Sum"), result.failingRows().get(0).failingOutputs());
        assertEquals(List.of("Sum"), result.failingOutputs());
        assertEquals(1, result.failingRows().get(0).index());
    }

    @Test
    void dontCareExpectedNeverFails() throws Exception {
        Circuit circuit = CircuitTestFactory.halfAdder();
        TruthTable expected = textFormat.parse("""
                inputs: A, B
                outputs: Sum, Cout
                0 0 | X X
                0 1 | X X
                1 0 | X X
                1 1 | X X
                """);
        TruthTableChecker checker = new TruthTableChecker(HALF_ADDER_EVALUATOR);

        CheckResult result = checker.check(circuit, expected);

        assertTrue(result.allPassed());
    }

    @Test
    void actualUnknownAgainstExpectedValueFails() throws Exception {
        Circuit circuit = CircuitTestFactory.halfAdder();
        TruthTable expected = textFormat.parse("""
                inputs: A, B
                outputs: Sum, Cout
                0 0 | 0 0
                """);
        CircuitEvaluator unknownOutputs = (c, inputs) -> Map.of("Sum", Logic.UNKNOWN, "Cout", Logic.UNKNOWN);
        TruthTableChecker checker = new TruthTableChecker(unknownOutputs);

        CheckResult result = checker.check(circuit, expected);

        assertFalse(result.allPassed());
        assertEquals(List.of("Sum", "Cout"), result.rows().get(0).failingOutputs());
    }

    @Test
    void differentCaseAndColumnOrderStillWork() throws Exception {
        Circuit circuit = CircuitTestFactory.halfAdder();
        TruthTable expected = textFormat.parse("""
                inputs: b, a
                outputs: Cout, Sum
                0 0 | 0 0
                0 1 | 0 1
                1 0 | 0 1
                1 1 | 1 0
                """);
        TruthTableChecker checker = new TruthTableChecker(HALF_ADDER_EVALUATOR);

        CheckResult result = checker.check(circuit, expected);

        assertTrue(result.allPassed());
        assertEquals(List.of("b", "a"), expected.inputLabels());
        assertEquals(List.of(Logic.ZERO, Logic.ZERO), result.rows().get(0).inputs());
        assertEquals(List.of(Logic.ZERO, Logic.ZERO), result.rows().get(0).expected());
        assertEquals(List.of(Logic.ZERO, Logic.ZERO), result.rows().get(0).actual());
    }

    @Test
    void inputLabelMismatchDoesNotCallEvaluator() {
        Circuit circuit = CircuitTestFactory.halfAdder();
        TruthTable expected = new TruthTable(List.of("A", "Cin"), List.of("Sum", "Cout"), List.of());
        AtomicBoolean called = new AtomicBoolean(false);
        CircuitEvaluator spy = (c, inputs) -> {
            called.set(true);
            return Map.of();
        };
        TruthTableChecker checker = new TruthTableChecker(spy);

        LabelMismatchException ex = assertThrows(LabelMismatchException.class,
                () -> checker.check(circuit, expected));

        assertEquals("Spec expects inputs {A, Cin} but circuit has {A, B}.", ex.getMessage());
        assertFalse(called.get());
    }

    @Test
    void outputLabelMismatchDoesNotCallEvaluator() {
        Circuit circuit = CircuitTestFactory.halfAdder();
        TruthTable expected = new TruthTable(List.of("A", "B"), List.of("Sum", "Carry"), List.of());
        AtomicBoolean called = new AtomicBoolean(false);
        CircuitEvaluator spy = (c, inputs) -> {
            called.set(true);
            return Map.of();
        };
        TruthTableChecker checker = new TruthTableChecker(spy);

        LabelMismatchException ex = assertThrows(LabelMismatchException.class,
                () -> checker.check(circuit, expected));

        assertEquals("Spec expects outputs {Sum, Carry} but circuit has {Cout, Sum}.", ex.getMessage());
        assertFalse(called.get());
    }

    @Test
    void notSimulatableExceptionPropagates() {
        Circuit circuit = CircuitTestFactory.halfAdder();
        TruthTable expected = new TruthTable(List.of("A", "B"), List.of("Sum", "Cout"),
                List.of(new TruthTable.Row(List.of(Logic.ZERO, Logic.ZERO), List.of(Logic.ZERO, Logic.ZERO))));
        CircuitEvaluator failing = (c, inputs) -> {
            throw new NotSimulatableException("loop");
        };
        TruthTableChecker checker = new TruthTableChecker(failing);

        NotSimulatableException ex = assertThrows(NotSimulatableException.class,
                () -> checker.check(circuit, expected));
        assertEquals("loop", ex.getMessage());
    }
}
