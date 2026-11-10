package entity.simulation;

import entity.circuit.GateType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateFunctionsTest {

    private final Map<GateType, GateFunction> functions = GateFunctions.standard();

    @Test
    void mapCoversEveryGateType() {
        for (GateType type : GateType.values()) {
            assertTrue(functions.containsKey(type));
        }
    }

    @Test
    void andWithUnknown() {
        assertEquals(Logic.ZERO, apply(GateType.AND, Logic.ZERO, Logic.UNKNOWN));
        assertEquals(Logic.UNKNOWN, apply(GateType.AND, Logic.ONE, Logic.UNKNOWN));
        assertEquals(Logic.ONE, apply(GateType.AND, Logic.ONE, Logic.ONE));
    }

    @Test
    void orWithUnknown() {
        assertEquals(Logic.ONE, apply(GateType.OR, Logic.ONE, Logic.UNKNOWN));
        assertEquals(Logic.UNKNOWN, apply(GateType.OR, Logic.ZERO, Logic.UNKNOWN));
        assertEquals(Logic.ZERO, apply(GateType.OR, Logic.ZERO, Logic.ZERO));
    }

    @Test
    void xorVersusXorOneWithThreeInputs() {
        assertEquals(Logic.ONE, apply(GateType.XOR, Logic.ONE, Logic.ONE, Logic.ONE));
        assertEquals(Logic.ZERO, apply(GateType.XOR_ONE, Logic.ONE, Logic.ONE, Logic.ONE));
    }

    @Test
    void inversesMatchNot() {
        assertEquals(apply(GateType.AND, Logic.ONE, Logic.ZERO).not(),
                apply(GateType.NAND, Logic.ONE, Logic.ZERO));
        assertEquals(apply(GateType.OR, Logic.ZERO, Logic.ONE).not(),
                apply(GateType.NOR, Logic.ZERO, Logic.ONE));
        assertEquals(apply(GateType.XOR, Logic.ONE, Logic.ZERO).not(),
                apply(GateType.XNOR, Logic.ONE, Logic.ZERO));
        assertEquals(apply(GateType.XOR_ONE, Logic.ONE, Logic.ZERO, Logic.ZERO).not(),
                apply(GateType.XNOR_ONE, Logic.ONE, Logic.ZERO, Logic.ZERO));
    }

    @Test
    void unaryGates() {
        assertEquals(Logic.ONE, apply(GateType.NOT, Logic.ZERO));
        assertEquals(Logic.ZERO, apply(GateType.BUFFER, Logic.ZERO));
        assertEquals(Logic.UNKNOWN, apply(GateType.NOT, Logic.UNKNOWN));
    }

    private Logic apply(GateType type, Logic... inputs) {
        return functions.get(type).apply(List.of(inputs));
    }
}
