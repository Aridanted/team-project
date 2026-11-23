package data_access.circ;

import entity.circuit.Gate;
import entity.circuit.Point;
import entity.circuit.Wire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.TreeSet;

/** Finds orthogonal wire paths that avoid gate bodies and accidental junctions. */
public final class WirePathFinder {
    private static final int GRID_SIZE = 10;
    private static final int SEARCH_MARGIN = 100;

    private final WireRoutingRules rules;
    private final GateObstacleChecker obstacles;

    /**
     * Creates a path finder using the supplied placement checks.
     *
     * @param rules the electrical connection checks
     * @param obstacles the gate body checks
     */
    public WirePathFinder(WireRoutingRules rules, GateObstacleChecker obstacles) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.obstacles = Objects.requireNonNull(obstacles, "obstacles");
    }

    /**
     * Finds a route between two ports on the same net.
     *
     * @param source the starting port
     * @param destination the destination port
     * @param net the net being routed
     * @param existing the wires already routed
     * @param portsByNet all circuit ports grouped by net
     * @param gates the positioned, normalized, east-facing gates
     * @return the wire segments connecting the two ports
     * @throws IllegalArgumentException if no safe route is found
     */
    public List<Wire> findPath(Point source, Point destination, int net,
                               List<Wire> existing, Map<Integer, List<Point>> portsByNet,
                               List<Gate> gates) {
        if (source.equals(destination)) {
            return List.of();
        }

        List<Point> points = new ArrayList<>();
        portsByNet.values().forEach(points::addAll);
        points.add(source);
        points.add(destination);
        for (Wire wire : existing) {
            points.add(wire.from());
            points.add(wire.to());
        }

        List<Integer> columns = coordinates(points, true);
        List<Integer> rows = coordinates(points, false);
        RoutingContext context = new RoutingContext(net, existing, portsByNet, gates);
        Map<Point, Point> previous = search(source, destination, columns, rows, context);
        return reconstruct(source, destination, previous, net);
    }

    private Map<Point, Point> search(Point source, Point destination, List<Integer> columns,
                                     List<Integer> rows, RoutingContext context) {
        PriorityQueue<SearchEntry> pending = new PriorityQueue<>(
                Comparator.comparingInt(SearchEntry::priority));
        Map<Point, Integer> distances = new HashMap<>();
        Map<Point, Point> previous = new HashMap<>();
        distances.put(source, 0);
        pending.add(new SearchEntry(source, 0, distance(source, destination)));

        while (!pending.isEmpty()) {
            SearchEntry current = pending.remove();
            if (current.distance() != distances.get(current.point())) {
                continue;
            }
            if (current.point().equals(destination)) {
                return previous;
            }

            for (Point next : neighbours(current.point(), columns, rows, context)) {
                int cost = current.distance() + distance(current.point(), next);
                if (cost < distances.getOrDefault(next, Integer.MAX_VALUE)) {
                    distances.put(next, cost);
                    previous.put(next, current.point());
                    pending.add(new SearchEntry(next, cost, cost + distance(next, destination)));
                }
            }
        }

        throw new IllegalArgumentException(
                "No safe wire route found from " + source + " to " + destination);
    }

    private List<Point> neighbours(Point from, List<Integer> columns, List<Integer> rows,
                                   RoutingContext context) {
        List<Point> result = new ArrayList<>();
        addNeighbour(result, nextPoint(from, columns, rows, 1, 0, context));
        addNeighbour(result, nextPoint(from, columns, rows, -1, 0, context));
        addNeighbour(result, nextPoint(from, columns, rows, 0, 1, context));
        addNeighbour(result, nextPoint(from, columns, rows, 0, -1, context));
        return result;
    }

    private Point nextPoint(Point from, List<Integer> columns, List<Integer> rows,
                            int dx, int dy, RoutingContext context) {
        int column = Collections.binarySearch(columns, from.x()) + dx;
        int row = Collections.binarySearch(rows, from.y()) + dy;

        while (column >= 0 && column < columns.size() && row >= 0 && row < rows.size()) {
            Point candidate = new Point(columns.get(column), rows.get(row));
            Wire segment = new Wire(from, candidate, context.net());

            if (!obstacles.canPlace(segment, context.gates())) {
                return null;
            }
            if (rules.canPlace(segment, context.existing(), context.portsByNet())) {
                return candidate;
            }

            // A crossing can be safe once the endpoint is beyond the other wire.
            column += dx;
            row += dy;
        }
        return null;
    }

    private void addNeighbour(List<Point> neighbours, Point candidate) {
        if (candidate != null) {
            neighbours.add(candidate);
        }
    }

    private List<Integer> coordinates(List<Point> points, boolean horizontal) {
        TreeSet<Integer> values = new TreeSet<>();
        for (Point point : points) {
            int coordinate = horizontal ? point.x() : point.y();
            for (int offset = -3 * GRID_SIZE; offset <= 3 * GRID_SIZE; offset += GRID_SIZE) {
                int value = coordinate + offset;
                if (value >= GRID_SIZE) {
                    values.add(value);
                }
            }
        }

        int minimum = values.first();
        int maximum = values.last();
        values.add(Math.max(GRID_SIZE, minimum - SEARCH_MARGIN));
        values.add(maximum + SEARCH_MARGIN);
        return List.copyOf(values);
    }

    private List<Wire> reconstruct(Point source, Point destination,
                                   Map<Point, Point> previous, int net) {
        List<Point> points = new ArrayList<>();
        Point current = destination;
        while (!current.equals(source)) {
            points.add(current);
            current = previous.get(current);
        }
        points.add(source);
        Collections.reverse(points);

        List<Point> bends = new ArrayList<>();
        for (Point point : points) {
            if (bends.size() >= 2 && collinear(bends.get(bends.size() - 2),
                    bends.getLast(), point)) {
                bends.set(bends.size() - 1, point);
            } else {
                bends.add(point);
            }
        }

        List<Wire> wires = new ArrayList<>();
        for (int index = 1; index < bends.size(); index++) {
            wires.add(new Wire(bends.get(index - 1), bends.get(index), net));
        }
        return List.copyOf(wires);
    }

    private boolean collinear(Point first, Point second, Point third) {
        return first.x() == second.x() && second.x() == third.x()
                || first.y() == second.y() && second.y() == third.y();
    }

    private int distance(Point first, Point second) {
        return Math.abs(first.x() - second.x()) + Math.abs(first.y() - second.y());
    }

    private record SearchEntry(Point point, int distance, int priority) {
    }

    private record RoutingContext(int net, List<Wire> existing,
                                  Map<Integer, List<Point>> portsByNet, List<Gate> gates) {
    }
}
