package entity.circuit;

/**
 * A position on the circuit's pixel grid. Used for drawing and for telling the user where
 * something is; the simulator never looks at positions.
 *
 * @param x horizontal coordinate, growing to the right
 * @param y vertical coordinate, growing downward
 */
public record Point(int x, int y) {

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
