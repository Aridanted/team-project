package data_access.circ;

import entity.circuit.Point;
import entity.circuit.Wire;

import java.util.List;
import java.util.Map;

/** Checks whether a wire segment would accidentally connect different nets. */
public final class WireRoutingRules {

    /**
     * Checks a proposed horizontal or vertical wire segment.
     *
     * @param candidate the proposed segment
     * @param existing the segments already routed
     * @param portsByNet all circuit ports grouped by net
     * @return whether the segment avoids connections to other nets
     * @throws IllegalArgumentException if the segment is diagonal or has zero length
     */
    public boolean canPlace(Wire candidate, List<Wire> existing,
                            Map<Integer, List<Point>> portsByNet) {
        requireValidSegment(candidate);

        for (Map.Entry<Integer, List<Point>> entry : portsByNet.entrySet()) {
            if (entry.getKey() != candidate.net()) {
                for (Point port : entry.getValue()) {
                    if (contains(candidate, port)) {
                        return false;
                    }
                }
            }
        }

        for (Wire wire : existing) {
            requireValidSegment(wire);
            if (candidate.net() != wire.net() && conflicts(candidate, wire)) {
                return false;
            }
        }
        return true;
    }

    private boolean conflicts(Wire first, Wire second) {
        boolean firstHorizontal = first.from().y() == first.to().y();
        boolean secondHorizontal = second.from().y() == second.to().y();

        if (firstHorizontal == secondHorizontal) {
            return contains(first, second.from()) || contains(first, second.to())
                    || contains(second, first.from()) || contains(second, first.to());
        }

        Wire horizontal = firstHorizontal ? first : second;
        Wire vertical = firstHorizontal ? second : first;
        Point crossing = new Point(vertical.from().x(), horizontal.from().y());

        if (!contains(horizontal, crossing) || !contains(vertical, crossing)) {
            return false;
        }

        return isEndpoint(horizontal, crossing) || isEndpoint(vertical, crossing);
    }

    private boolean contains(Wire wire, Point point) {
        boolean horizontal = wire.from().y() == wire.to().y();
        boolean aligned = horizontal
                ? point.y() == wire.from().y()
                : point.x() == wire.from().x();

        return aligned
                && point.x() >= Math.min(wire.from().x(), wire.to().x())
                && point.x() <= Math.max(wire.from().x(), wire.to().x())
                && point.y() >= Math.min(wire.from().y(), wire.to().y())
                && point.y() <= Math.max(wire.from().y(), wire.to().y());
    }

    private boolean isEndpoint(Wire wire, Point point) {
        return point.equals(wire.from()) || point.equals(wire.to());
    }

    private void requireValidSegment(Wire wire) {
        if (wire.from().equals(wire.to())) {
            throw new IllegalArgumentException("A wire segment must have a nonzero length.");
        }
        if (wire.from().x() != wire.to().x() && wire.from().y() != wire.to().y()) {
            throw new IllegalArgumentException("A wire segment must be horizontal or vertical.");
        }
    }
}
