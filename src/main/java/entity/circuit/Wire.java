package entity.circuit;

import java.util.Objects;

/**
 * A straight wire segment, kept for drawing.
 *
 * @param net the net the segment belongs to
 */
public record Wire(Point from, Point to, int net) {

    public Wire {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
    }
}
