package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.GateType;
import entity.circuit.Point;
import entity.circuit.Wire;
import entity.design.CircuitDesign;
import entity.design.GateSpec;
import org.junit.jupiter.api.Test;
import use_case.build_circuit.DesignAssembler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitWireRouterTest {
    private final GatePortLocator gatePorts = new GatePortLocator();
    private final CircuitPortCollector collector = new CircuitPortCollector(gatePorts);
    private final WireRoutingRules rules = new WireRoutingRules();
    private final GateObstacleChecker obstacles = new GateObstacleChecker(gatePorts);
    private final CircuitWireRouter router = new CircuitWireRouter(
            collector, new WirePathFinder(rules, obstacles));
    private final CircuitLayout layout = new CircuitLayout(new GateDepthCalculator());
    private final DesignAssembler assembler = new DesignAssembler();

    @Test
    void routesADirectInputToOutputConnection() {
        CircuitDesign design = new CircuitDesign("Direct", List.of("A"), List.of("Y"),
                List.of(), Map.of("Y", "A"));
        Circuit positioned = position(design);

        Circuit routed = router.route(positioned);

        assertFalse(routed.wires().isEmpty());
        assertEquals(positioned.components(), routed.components());
        assertEquals(positioned.netCount(), routed.netCount());
        assertTrue(positioned.wires().isEmpty());
        assertCorrectConnections(routed);
    }

    @Test
    void routesAHalfAdderWithSharedInputsAndSeparateOutputs() {
        CircuitDesign design = halfAdder();

        Circuit routed = router.route(position(design));

        assertCorrectConnections(routed);
    }

    @Test
    void routesAConstantAndANormalizedUnaryGate() {
        CircuitDesign design = new CircuitDesign("Inverter", List.of("A"), List.of("Y"),
                List.of(
                        new GateSpec("G2", GateType.AND, List.of("G1", "1")),
                        new GateSpec("G1", GateType.BUFFER, List.of("!A"))),
                Map.of("Y", "G2"));

        Circuit routed = router.route(position(design));

        assertCorrectConnections(routed);
    }

    @Test
    void producesTheSameWiresWhenRoutingTheSameCircuitAgain() {
        Circuit positioned = position(halfAdder());

        Circuit first = router.route(positioned);
        Circuit second = router.route(positioned);

        assertEquals(first.wires(), second.wires());
        assertCorrectConnections(second);
    }

    private void assertCorrectConnections(Circuit circuit) {
        Map<Integer, List<Point>> portsByNet = collector.collect(circuit);
        List<Point> allPorts = portsByNet.values().stream().flatMap(List::stream).toList();
        List<RawCircuit.RawWire> rawWires = circuit.wires().stream()
                .map(wire -> new RawCircuit.RawWire(wire.from(), wire.to()))
                .toList();
        NetMap rebuilt = new NetBuilder().build(rawWires, allPorts);

        Set<Integer> separateNets = new HashSet<>();
        for (List<Point> ports : portsByNet.values()) {
            int rebuiltNet = rebuilt.netAt(ports.getFirst());
            assertTrue(separateNets.add(rebuiltNet), "Different logical nets became connected");
            for (Point port : ports) {
                assertEquals(rebuiltNet, rebuilt.netAt(port), "A destination was disconnected");
            }
        }

        assertEquals(portsByNet.size(), rebuilt.netCount());
        assertTrue(rebuilt.danglingWireEnds().isEmpty());

        for (Wire wire : circuit.wires()) {
            assertTrue(wire.from().x() == wire.to().x()
                    || wire.from().y() == wire.to().y());
            assertTrue(rules.canPlace(wire, circuit.wires(), portsByNet));
            assertTrue(obstacles.canPlace(wire, circuit.gates()));
        }
    }

    private Circuit position(CircuitDesign design) {
        return layout.layout(assembler.assemble(design));
    }

    private static CircuitDesign halfAdder() {
        return new CircuitDesign("Half adder", List.of("A", "B"), List.of("Sum", "Cout"),
                List.of(
                        new GateSpec("G1", GateType.XOR, List.of("A", "B")),
                        new GateSpec("G2", GateType.AND, List.of("A", "B"))),
                Map.of("Sum", "G1", "Cout", "G2"));
    }
}
