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

    private static Logic and(List<Logic> inputs) {
        for (Logic input : inputs) {
            if (input == Logic.ZERO) {
                return Logic.ZERO;
            }
        }
        for (Logic input : inputs) {
            if (input == Logic.UNKNOWN) {
                return Logic.UNKNOWN;
            }
        }
        return Logic.ONE;
    }

    private static Logic or(List<Logic> inputs) {
        for (Logic input : inputs) {
            if (input == Logic.ONE) {
                return Logic.ONE;
            }
        }
        for (Logic input : inputs) {
            if (input == Logic.UNKNOWN) {
                return Logic.UNKNOWN;
            }
        }
        return Logic.ZERO;
    }

    private static Logic xor(List<Logic> inputs) {
        for (Logic input : inputs) {
            if (input == Logic.UNKNOWN) {
                return Logic.UNKNOWN;
            }
        }
        int ones = 0;
        for (Logic input : inputs) {
            if (input == Logic.ONE) {
                ones++;
            }
        }
        return (ones & 1) == 1 ? Logic.ONE : Logic.ZERO;
    }

    private static Logic xorOne(List<Logic> inputs) {
        for (Logic input : inputs) {
            if (input == Logic.UNKNOWN) {
                return Logic.UNKNOWN;
            }
        }
        int ones = 0;
        for (Logic input : inputs) {
            if (input == Logic.ONE) {
                ones++;
            }
        }
        return ones == 1 ? Logic.ONE : Logic.ZERO;
    }
}
