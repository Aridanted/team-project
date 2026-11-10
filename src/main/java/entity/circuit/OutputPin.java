package entity.circuit;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/** A circuit output: reports the value of its net under {@link #label()}. */
public record OutputPin(String id, String label, int net, Point location, Orientation facing)
        implements Pin {

    public OutputPin {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(facing, "facing");
    }

    @Override
    public List<Integer> inputNets() {
        return List.of(net);
    }

    @Override
    public OptionalInt drivenNet() {
        return OptionalInt.empty();
    }

    @Override
    public OutputPin withLabel(String newLabel) {
        return new OutputPin(id, newLabel, net, location, facing);
    }
}
