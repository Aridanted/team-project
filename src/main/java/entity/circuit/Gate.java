package entity.circuit;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * A logic gate.
 *
 * @param inputs    the gate's inputs in port order (top to bottom, or left to right when the gate
 *                  faces north or south)
 * @param outputNet the net the gate drives
 */
public record Gate(String id, GateType type, List<GateInput> inputs, int outputNet,
                   Point location, Orientation facing) implements CircuitComponent {

    public Gate {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(facing, "facing");
        inputs = List.copyOf(inputs);
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("a gate needs at least one input");
        }
        if (type.isUnary() && inputs.size() != 1) {
            throw new IllegalArgumentException(type + " takes exactly one input, got " + inputs.size());
        }
    }

    @Override
    public List<Integer> inputNets() {
        return inputs.stream().map(GateInput::net).toList();
    }

    @Override
    public OptionalInt drivenNet() {
        return OptionalInt.of(outputNet);
    }
}
