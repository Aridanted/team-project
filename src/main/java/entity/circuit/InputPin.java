package entity.circuit;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/** A circuit input: drives its net with the value the user assigns to {@link #label()}. */
public record InputPin(String id, String label, int net, Point location, Orientation facing)
        implements Pin {

    public InputPin {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(facing, "facing");
    }

    @Override
    public List<Integer> inputNets() {
        return List.of();
    }

    @Override
    public OptionalInt drivenNet() {
        return OptionalInt.of(net);
    }

    @Override
    public InputPin withLabel(String newLabel) {
        return new InputPin(id, newLabel, net, location, facing);
    }
}
