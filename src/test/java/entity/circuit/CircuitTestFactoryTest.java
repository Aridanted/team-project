package entity.circuit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitTestFactoryTest {

    @Test
    void halfAdderIsWiredBySignalName() {
        Circuit circuit = CircuitTestFactory.halfAdder();
        int a = circuit.inputPins().get(0).net();
        int b = circuit.inputPins().get(1).net();
        Gate xor = circuit.gates().get(0);

        assertEquals(List.of(a, b), xor.inputNets());
        assertEquals(xor.outputNet(), circuit.outputPins().get(1).net());
        assertEquals(List.of("Cout", "Sum"), circuit.outputPins().stream().map(Pin::label).toList());
    }

    @Test
    void supportsNegationFeedbackAndFloatingSignals() {
        Circuit circuit = CircuitTestFactory.builder("loop")
                .input("A")
                .gate("g1", GateType.AND, "!A", "g2")
                .gate("g2", GateType.NOT, "g1")
                .output("Y", "nowhere")
                .build();

        Gate g1 = circuit.gates().get(0);
        Gate g2 = circuit.gates().get(1);
        assertTrue(g1.inputs().get(0).negated());
        assertEquals(g2.outputNet(), g1.inputs().get(1).net());
        assertEquals(4, circuit.netCount());
    }

    @Test
    void rejectsDefiningASignalTwice() {
        assertThrows(IllegalArgumentException.class,
                () -> CircuitTestFactory.builder("x").input("A").input("A"));
    }
}
