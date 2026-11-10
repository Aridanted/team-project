package entity.simulation;

import entity.circuit.GateType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Standard three-valued logic functions for every {@link GateType}. */
public final class GateFunctions {

    private GateFunctions() {
    }

    /**
     * @return an immutable map from gate type to its standard logic function
     */
    public static Map<GateType, GateFunction> standard() {
        EnumMap<GateType, GateFunction> map = new EnumMap<>(GateType.class);
        map.put(GateType.AND, GateFunctions::and);
        map.put(GateType.OR, GateFunctions::or);
        map.put(GateType.XOR, GateFunctions::xor);
        map.put(GateType.XOR_ONE, GateFunctions::xorOne);
        map.put(GateType.NAND, inputs -> and(inputs).not());
        map.put(GateType.NOR, inputs -> or(inputs).not());
        map.put(GateType.XNOR, inputs -> xor(inputs).not());
        map.put(GateType.XNOR_ONE, inputs -> xorOne(inputs).not());
        map.put(GateType.NOT, inputs -> inputs.getFirst().not());
        map.put(GateType.BUFFER, inputs -> inputs.getFirst());
        return Map.copyOf(map);
    }

    /** A single 0 decides the result even if other inputs are unknown. */
    private static Logic and(List<Logic> inputs) {
        if (inputs.contains(Logic.ZERO)) {
            return Logic.ZERO;
        }
        return inputs.contains(Logic.UNKNOWN) ? Logic.UNKNOWN : Logic.ONE;
    }

    /** A single 1 decides the result even if other inputs are unknown. */
    private static Logic or(List<Logic> inputs) {
        if (inputs.contains(Logic.ONE)) {
            return Logic.ONE;
        }
        return inputs.contains(Logic.UNKNOWN) ? Logic.UNKNOWN : Logic.ZERO;
    }

    private static Logic xor(List<Logic> inputs) {
        return inputs.contains(Logic.UNKNOWN) ? Logic.UNKNOWN : Logic.of(countOnes(inputs) % 2 == 1);
    }

    private static Logic xorOne(List<Logic> inputs) {
        return inputs.contains(Logic.UNKNOWN) ? Logic.UNKNOWN : Logic.of(countOnes(inputs) == 1);
    }

    private static long countOnes(List<Logic> inputs) {
        return inputs.stream().filter(input -> input == Logic.ONE).count();
    }
}
