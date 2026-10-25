package data_access.circ;

import entity.circuit.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Works out which points are electrically connected, following Logisim's rules (confirmed
 * against Logisim-evolution 3.7.2):
 * <ul>
 *   <li>a wire connects its two endpoints;</li>
 *   <li>a wire endpoint or a component port lying on the interior of another wire connects to it
 *       (a T-junction);</li>
 *   <li>two wires that merely cross, with no endpoint or port at the crossing, do not connect.</li>
 * </ul>
 * Every port gets a net, even if nothing is attached, so an unconnected input simply has a net
 * with no driver.
 */
final class NetBuilder {

    private static final Comparator<Point> BY_POSITION =
            Comparator.comparingInt(Point::x).thenComparingInt(Point::y);

    NetMap build(List<RawCircuit.RawWire> wires, Collection<Point> ports) {
        UnionFind sets = new UnionFind();
        Map<Point, Integer> endpointUses = new HashMap<>();
        Set<Point> points = new LinkedHashSet<>(ports);
        for (RawCircuit.RawWire wire : wires) {
            points.add(wire.from());
            points.add(wire.to());
            endpointUses.merge(wire.from(), 1, Integer::sum);
            endpointUses.merge(wire.to(), 1, Integer::sum);
        }
        points.forEach(sets::add);

        Set<Point> onAnotherWire = new HashSet<>();
        for (RawCircuit.RawWire wire : wires) {
            sets.union(wire.from(), wire.to());
            for (Point point : points) {
                if (liesStrictlyInside(point, wire)) {
                    sets.union(point, wire.from());
                    onAnotherWire.add(point);
                }
            }
        }

        Map<Point, Integer> netByPoint = numberNets(points, sets);
        Set<Integer> wiredNets = new HashSet<>();
        for (RawCircuit.RawWire wire : wires) {
            wiredNets.add(netByPoint.get(wire.from()));
        }
        Set<Point> portSet = new HashSet<>(ports);
        List<Point> dangling = endpointUses.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .filter(p -> !portSet.contains(p) && !onAnotherWire.contains(p))
                .sorted(BY_POSITION)
                .toList();
        return new NetMap(netByPoint, new HashSet<>(netByPoint.values()).size(), wiredNets, dangling);
    }

    /** Numbers nets deterministically, ordered by each net's smallest point. */
    private static Map<Point, Integer> numberNets(Set<Point> points, UnionFind sets) {
        Map<Point, List<Point>> membersByRoot = new HashMap<>();
        for (Point point : points) {
            membersByRoot.computeIfAbsent(sets.find(point), root -> new ArrayList<>()).add(point);
        }
        TreeMap<Point, List<Point>> bySmallestMember = new TreeMap<>(BY_POSITION);
        for (List<Point> members : membersByRoot.values()) {
            bySmallestMember.put(members.stream().min(BY_POSITION).orElseThrow(), members);
        }
        Map<Point, Integer> netByPoint = new HashMap<>();
        int net = 0;
        for (List<Point> members : bySmallestMember.values()) {
            for (Point member : members) {
                netByPoint.put(member, net);
            }
            net++;
        }
        return netByPoint;
    }

    private static boolean liesStrictlyInside(Point p, RawCircuit.RawWire wire) {
        Point a = wire.from();
        Point b = wire.to();
        if (p.equals(a) || p.equals(b)) {
            return false;
        }
        long cross = (long) (b.x() - a.x()) * (p.y() - a.y()) - (long) (b.y() - a.y()) * (p.x() - a.x());
        return cross == 0
                && p.x() >= Math.min(a.x(), b.x()) && p.x() <= Math.max(a.x(), b.x())
                && p.y() >= Math.min(a.y(), b.y()) && p.y() <= Math.max(a.y(), b.y());
    }

    private static final class UnionFind {
        private final Map<Point, Point> parent = new HashMap<>();

        void add(Point point) {
            parent.putIfAbsent(point, point);
        }

        Point find(Point point) {
            Point root = point;
            while (!parent.get(root).equals(root)) {
                root = parent.get(root);
            }
            Point current = point;
            while (!current.equals(root)) {
                Point next = parent.get(current);
                parent.put(current, root);
                current = next;
            }
            return root;
        }

        void union(Point a, Point b) {
            Point rootA = find(a);
            Point rootB = find(b);
            if (!rootA.equals(rootB)) {
                parent.put(rootA, rootB);
            }
        }
    }
}
