package data_access.circ;

import entity.circuit.Orientation;
import entity.circuit.Point;

import java.util.List;
import java.util.Map;

/**
 * Where Logisim puts gate ports. Logisim does not store port positions in the file, so they are
 * computed from the component's location, attributes and facing.
 *
 * <p>All numbers were measured from Logisim-evolution 3.7.2 itself (its component factories
 * report the exact port locations) and are pinned by {@code GateGeometryTest}. If a different
 * Logisim version places ports differently, that test is the place to start.
 */
final class GateGeometry {

    /** Default {@code size} attribute of multi-input gates (Logisim's "medium"). */
    static final int DEFAULT_SIZE = 50;

    /** Default {@code inputs} attribute of multi-input gates. */
    static final int DEFAULT_INPUTS = 2;

    /** Extra distance a negated input sits from the gate body (the inversion bubble). */
    static final int NEGATION_BUBBLE = 10;

    /** Gate sizes whose inputs spread wider than the usual 10 px spacing, by input count. */
    private static final Map<Integer, Map<Integer, List<Integer>>> WIDE_SPACING = Map.of(
            50, Map.of(
                    2, List.of(-20, 20),
                    3, List.of(-20, 0, 20)),
            70, Map.of(
                    2, List.of(-20, 20),
                    3, List.of(-30, 0, 30),
                    4, List.of(-30, -10, 10, 30)));

    private GateGeometry() {
    }

    /**
     * @return the perpendicular offset of input {@code index} from the gate's output point, for
     *         a gate facing east (negative is up). Inputs are 10 px apart and, for an even count,
     *         skip the centre line, unless the size spreads them wider.
     */
    static int inputOffset(int size, int inputCount, int index) {
        List<Integer> wide = WIDE_SPACING.getOrDefault(size, Map.of()).get(inputCount);
        if (wide != null) {
            return wide.get(index);
        }
        int half = inputCount / 2;
        if (inputCount % 2 == 1) {
            return (index - half) * 10;
        }
        return (index < half ? index - half : index - half + 1) * 10;
    }

    /**
     * Places an offset given for an east-facing component relative to {@code origin}, rotated to
     * {@code facing}. Matches Logisim: a west-facing gate keeps its input order top to bottom, and
     * north/south-facing gates order inputs left to right.
     */
    static Point place(Point origin, int dx, int dy, Orientation facing) {
        return switch (facing) {
            case EAST -> new Point(origin.x() + dx, origin.y() + dy);
            case WEST -> new Point(origin.x() - dx, origin.y() + dy);
            case NORTH -> new Point(origin.x() + dy, origin.y() - dx);
            case SOUTH -> new Point(origin.x() + dy, origin.y() + dx);
        };
    }
}
