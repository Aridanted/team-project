package entity.circuit;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/** A fixed 0 or 1 source (Logisim Constant, Power or Ground). */
public record Constant(String id, boolean value, int net, Point location, Orientation facing)
        implements CircuitComponent {

    public Constant {
        Objects.requireNonNull(id, "id");
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
}
