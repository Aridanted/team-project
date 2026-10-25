package entity.truthtable;

import entity.circuit.Circuit;
import entity.circuit.CircuitTestFactory;
import entity.circuit.GateType;
import entity.simulation.CircuitEvaluator;
import entity.simulation.CircuitSimulator;
import entity.simulation.Logic;
import entity.simulation.NotSimulatableException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TruthTableGeneratorTest {

    @Test
    void halfAdderTableRowsAndLabels() throws NotSimulatableException {
        Circuit circuit = CircuitTestFactory.halfAdder();
        TruthTable table = new TruthTableGenerator(new CircuitSimulator()).generate(circuit);

        assertEquals(List.of("A", "B"), table.inputLabels());
        assertEquals(List.of("Cout", "Sum"), table.outputLabels());
        assertEquals(4, table.rows().size());

        assertRow(table.rows().get(0), Logic.ZERO, Logic.ZERO, Logic.ZERO, Logic.ZERO);
        assertRow(table.rows().get(1), Logic.ZERO, Logic.ONE, Logic.ZERO, Logic.ONE);
        assertRow(table.rows().get(2), Logic.ONE, Logic.ZERO, Logic.ZERO, Logic.ONE);
        assertRow(table.rows().get(3), Logic.ONE, Logic.ONE, Logic.ONE, Logic.ZERO);
    }

    @Test
    void tooManyInputsThrows() {
        var builder = CircuitTestFactory.builder("many");
        for (int i = 0; i < 11; i++) {
            builder.input("I" + i);
        }
        builder.output("Y", "I0");
        Circuit circuit = builder.build();

        NotSimulatableException ex = assertThrows(NotSimulatableException.class,
                () -> new TruthTableGenerator(new CircuitSimulator()).generate(circuit));
        assertEquals("The circuit has 11 inputs; at most 10 are supported.", ex.getMessage());
    }

    @Test
    void usesInjectedEvaluator() throws NotSimulatableException {
        Circuit circuit = CircuitTestFactory.builder("stub")
                .input("A")
                .gate("y", GateType.BUFFER, "A")
                .output("Y", "y")
                .build();
        AtomicInteger calls = new AtomicInteger();
        CircuitEvaluator fake = (c, inputs) -> {
            calls.incrementAndGet();
            return Map.of("Y", Logic.ONE);
        };
        TruthTable table = new TruthTableGenerator(fake).generate(circuit);
        assertEquals(2, calls.get());
        assertTrue(table.rows().stream().allMatch(row -> row.outputs().equals(List.of(Logic.ONE))));
    }

    private static void assertRow(TruthTable.Row row, Logic a, Logic b, Logic cout, Logic sum) {
        assertEquals(List.of(a, b), row.inputs());
        assertEquals(List.of(cout, sum), row.outputs());
    }
}
