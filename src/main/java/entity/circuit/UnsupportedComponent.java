package entity.circuit;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * A component outside the supported subset. It is kept so it can be shown to the user, but its
 * presence makes the circuit non-simulatable. Its connections are unknown.
 *
 * @param name   the component's name in the source file, for example "D Flip-Flop"
 * @param reason why it is unsupported, or what to use instead; may be empty
 */
public record UnsupportedComponent(String id, String name, String reason, Point location,
                                   Orientation facing) implements CircuitComponent {

    public UnsupportedComponent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(facing, "facing");
    }

    @Override
    public List<Integer> inputNets() {
        return List.of();
    }

    @Override
    public OptionalInt drivenNet() {
        return OptionalInt.empty();
    }
}
