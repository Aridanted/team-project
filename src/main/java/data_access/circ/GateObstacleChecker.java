package data_access.circ;

import entity.circuit.Gate;
import entity.circuit.Orientation;
import entity.circuit.Point;
import entity.circuit.Wire;

import java.util.List;
import java.util.Objects;

/** Checks whether a wire segment passes through a gate body or input bubble. */
public final class GateObstacleChecker {
    private static final int UNARY_HALF_HEIGHT = 20;
    private static final int MULTI_INPUT_HALF_HEIGHT = 30;
    private static final int PORT_MARGIN = 10;
    private static final int BUBBLE_HALF_HEIGHT = 5;

    private final GatePortLocator gatePorts;

    /**
     * Creates the obstacle checker.
     *
     * @param gatePorts the locator for normalized gate input ports
     */
    public GateObstacleChecker(GatePortLocator gatePorts) {
        this.gatePorts = Objects.requireNonNull(gatePorts, "gatePorts");
    }

    /**
     * Checks a horizontal or vertical segment against all gate bodies.
     *
     * @param wire the proposed wire segment
     * @param gates the positioned, east-facing gates
     * @return whether the segment avoids every gate body and input bubble
     * @throws IllegalArgumentException if a gate is not facing east or the wire is diagonal
     */
    public boolean canPlace(Wire wire, List<Gate> gates) {
        if (wire.from().x() != wire.to().x() && wire.from().y() != wire.to().y()) {
            throw new IllegalArgumentException("A wire segment must be horizontal or vertical.");
        }

        for (Gate gate : gates) {
            if (gate.facing() != Orientation.EAST) {
                throw new IllegalArgumentException("Run CircuitLayout before routing wires.");
            }
            if (crossesGate(wire, gate)) {
                return false;
            }
        }
        return true;
    }

    private boolean crossesGate(Wire wire, Gate gate) {
        List<Point> ports = gatePorts.inputPorts(gate);
        int left = gate.location().x();
        int halfHeight = gate.type().isUnary() ? UNARY_HALF_HEIGHT : MULTI_INPUT_HALF_HEIGHT;

        for (int index = 0; index < ports.size(); index++) {
            Point port = ports.get(index);
            int bodyInputX = port.x();

            if (gate.inputs().get(index).negated()) {
                bodyInputX += GateGeometry.NEGATION_BUBBLE;
                if (crossesRectangle(wire, port.x(), bodyInputX,
                        port.y() - BUBBLE_HALF_HEIGHT, port.y() + BUBBLE_HALF_HEIGHT)) {
                    return true;
                }
            }

            left = Math.min(left, bodyInputX);
            halfHeight = Math.max(halfHeight,
                    Math.abs(port.y() - gate.location().y()) + PORT_MARGIN);
        }

        return crossesRectangle(wire, left, gate.location().x(),
                gate.location().y() - halfHeight, gate.location().y() + halfHeight);
    }

    private boolean crossesRectangle(Wire wire, int left, int right, int top, int bottom) {
        if (wire.from().y() == wire.to().y()) {
            int y = wire.from().y();
            return y > top && y < bottom
                    && Math.max(wire.from().x(), wire.to().x()) > left
                    && Math.min(wire.from().x(), wire.to().x()) < right;
        }

        int x = wire.from().x();
        return x > left && x < right
                && Math.max(wire.from().y(), wire.to().y()) > top
                && Math.min(wire.from().y(), wire.to().y()) < bottom;
    }
}
