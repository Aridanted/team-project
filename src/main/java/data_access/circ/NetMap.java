package data_access.circ;

import entity.circuit.Point;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The result of connectivity analysis.
 *
 * @param netByPoint       net id of every wire endpoint and component port
 * @param netCount         nets are numbered {@code 0..netCount-1}
 * @param wiredNets        nets that contain at least one wire
 * @param danglingWireEnds wire endpoints that touch nothing else
 */
record NetMap(Map<Point, Integer> netByPoint, int netCount, Set<Integer> wiredNets,
              List<Point> danglingWireEnds) {

    NetMap {
        netByPoint = Map.copyOf(netByPoint);
        wiredNets = Set.copyOf(wiredNets);
        danglingWireEnds = List.copyOf(danglingWireEnds);
    }

    int netAt(Point point) {
        Integer net = netByPoint.get(point);
        if (net == null) {
            throw new IllegalArgumentException("no net was built for " + point);
        }
        return net;
    }

    boolean hasWire(int net) {
        return wiredNets.contains(net);
    }
}
