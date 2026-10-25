package entity.simulation;

import entity.circuit.Circuit;
import entity.circuit.CircuitTestFactory;
import entity.circuit.GateType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircuitSimulatorTest {

    private final CircuitSimulator simulator = new CircuitSimulator();

    @Test
    void halfAdderAllRows() throws NotSimulatableException {
        Circuit circuit = CircuitTestFactory.halfAdder();
        assertRow(circuit, Map.of("A", Logic.ZERO, "B", Logic.ZERO), Map.of("Cout", Logic.ZERO, "Sum", Logic.ZERO));
        assertRow(circuit, Map.of("A", Logic.ZERO, "B", Logic.ONE), Map.of("Cout", Logic.ZERO, "Sum", Logic.ONE));
        assertRow(circuit, Map.of("A", Logic.ONE, "B", Logic.ZERO), Map.of("Cout", Logic.ZERO, "Sum", Logic.ONE));
        assertRow(circuit, Map.of("A", Logic.ONE, "B", Logic.ONE), Map.of("Cout", Logic.ONE, "Sum", Logic.ZERO));
    }

    @Test
    void fullAdderAllRows() throws NotSimulatableException {
        Circuit circuit = CircuitTestFactory.fullAdder();
        for (int a = 0; a <= 1; a++) {
            for (int b = 0; b <= 1; b++) {
                for (int cin = 0; cin <= 1; cin++) {
                    int sum = a ^ b ^ cin;
                    int cout = (a & b) | (b & cin) | (a & cin);
                    assertRow(circuit,
                            Map.of("A", Logic.of(a == 1), "B", Logic.of(b == 1), "Cin", Logic.of(cin == 1)),
                            Map.of("Cout", Logic.of(cout == 1), "Sum", Logic.of(sum == 1)));
                }
            }
        }
    }

    @Test
    void negatedInput() throws NotSimulatableException {
        Circuit circuit = CircuitTestFactory.builder("not input")
                .input("A")
                .gate("y", GateType.BUFFER, "!A")
                .output("Y", "y")
                .build();
        assertEquals(Map.of("Y", Logic.ONE), simulator.evaluate(circuit, Map.of("A", Logic.ZERO)));
        assertEquals(Map.of("Y", Logic.ZERO), simulator.evaluate(circuit, Map.of("A", Logic.ONE)));
    }

    @Test
    void constantsDriveNets() throws NotSimulatableException {
        Circuit circuit = CircuitTestFactory.builder("const")
                .constant("v", true)
                .gate("y", GateType.BUFFER, "v")
                .output("Y", "y")
                .build();
        assertEquals(Map.of("Y", Logic.ONE), simulator.evaluate(circuit, Map.of()));
    }

    @Test
    void floatingOutputIsUnknown() throws NotSimulatableException {
        Circuit circuit = CircuitTestFactory.builder("float")
                .input("A")
                .output("Y", "nowhere")
                .build();
        assertEquals(Map.of("Y", Logic.UNKNOWN), simulator.evaluate(circuit, Map.of("A", Logic.ONE)));
    }

    @Test
    void missingInputPropagatesUnknown() throws NotSimulatableException {
        Circuit circuit = CircuitTestFactory.halfAdder();
        Map<String, Logic> partial = Map.of("A", Logic.ONE);
        Map<String, Logic> out = simulator.evaluate(circuit, partial);
        assertEquals(Logic.UNKNOWN, out.get("Sum"));
        assertEquals(Logic.UNKNOWN, out.get("Cout"));
    }

    @Test
    void feedbackLoopThrowsExactMessage() {
        Circuit circuit = CircuitTestFactory.builder("loop")
                .input("A")
                .gate("g1", GateType.AND, "!A", "g2")
                .gate("g2", GateType.NOT, "g1")
                .output("Y", "g2")
                .build();
        NotSimulatableException ex = assertThrows(NotSimulatableException.class,
                () -> simulator.evaluate(circuit, Map.of("A", Logic.ZERO)));
        assertEquals("The circuit has a feedback loop, so it is not combinational.", ex.getMessage());
    }

    @Test
    void gateFeedingItselfIsAFeedbackLoop() {
        Circuit circuit = CircuitTestFactory.builder("self loop")
                .input("A")
                .gate("g", GateType.OR, "A", "g")
                .output("Y", "g")
                .build();
        assertThrows(NotSimulatableException.class,
                () -> simulator.evaluate(circuit, Map.of("A", Logic.ONE)));
    }

    @Test
    void unsupportedComponentThrows() {
        Circuit circuit = CircuitTestFactory.builder("bad")
                .input("A")
                .unsupported("D Flip-Flop")
                .output("Y", "A")
                .build();
        NotSimulatableException ex = assertThrows(NotSimulatableException.class,
                () -> simulator.evaluate(circuit, Map.of("A", Logic.ZERO)));
        assertEquals("The circuit contains unsupported components: D Flip-Flop.", ex.getMessage());
    }

    @Test
    void customGateFunctionsAreUsed() throws NotSimulatableException {
        EnumMap<GateType, GateFunction> custom = new EnumMap<>(GateFunctions.standard());
        custom.put(GateType.AND, inputs -> Logic.ONE);
        CircuitSimulator customSimulator = new CircuitSimulator(custom);
        Circuit circuit = CircuitTestFactory.halfAdder();
        Map<String, Logic> out = customSimulator.evaluate(circuit, Map.of("A", Logic.ZERO, "B", Logic.ZERO));
        assertEquals(Logic.ONE, out.get("Cout"));
    }

    @Test
    void outputOrderMatchesCircuitOutputPins() throws NotSimulatableException {
        Circuit circuit = CircuitTestFactory.halfAdder();
        Map<String, Logic> out = simulator.evaluate(circuit, Map.of("A", Logic.ONE, "B", Logic.ONE));
        assertEquals(
                circuit.outputPins().stream().map(entity.circuit.Pin::label).toList(),
                List.copyOf(out.keySet()));
    }

    private void assertRow(Circuit circuit, Map<String, Logic> inputs, Map<String, Logic> expected)
            throws NotSimulatableException {
        assertEquals(expected, simulator.evaluate(circuit, inputs));
    }
}
