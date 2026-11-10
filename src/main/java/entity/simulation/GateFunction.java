package entity.simulation;

import java.util.List;

/**
 * The combinational behaviour of a gate type. Each input is the value read from the corresponding
 * net after optional negation has been applied.
 */
@FunctionalInterface
public interface GateFunction {

    /**
     * @param inputs gate inputs in port order, negation already applied
     * @return the output value
     */
    Logic apply(List<Logic> inputs);
}
