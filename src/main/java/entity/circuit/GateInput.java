package entity.circuit;

/**
 * One input of a gate.
 *
 * @param net     the net the input reads
 * @param negated whether the input is inverted before the gate function is applied
 */
public record GateInput(int net, boolean negated) {
}
