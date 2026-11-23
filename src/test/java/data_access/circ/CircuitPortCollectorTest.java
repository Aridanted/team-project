package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.GateType;
import entity.circuit.InputPin;
import entity.circuit.Orientation;
import entity.circuit.OutputPin;
import entity.circuit.Point;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircuitPortCollectorTest {
    private final CircuitPortCollector collector = new CircuitPortCollector(new GatePortLocator());

    @Test
    void collectsBothEndsOfADirectConnection() {
        Circuit circuit = new Circuit("Direct", List.of(
                input("A", 0, new Point(100, 200)),
                output("Y", 0, new Point(340, 200))),
                List.of(), 1, List.of());

        assertEquals(Map.of(0, List.of(new Point(100, 200), new Point(340, 200))),
                collector.collect(circuit));
    }

    @Test
    void putsTheSourceFirstEvenWhenTheGateIsListedBeforeIt() {
        Gate gate = new Gate("G1", GateType.NOT, List.of(new GateInput(0, false)),
                1, new Point(340, 200), Orientation.EAST);
        Circuit circuit = new Circuit("Inverter", List.of(
                gate,
                output("Y", 1, new Point(580, 200)),
                input("A", 0, new Point(100, 200))),
                List.of(), 2, List.of());

        Map<Integer, List<Point>> ports = collector.collect(circuit);

        assertEquals(List.of(new Point(100, 200), new Point(310, 200)), ports.get(0));
        assertEquals(List.of(new Point(340, 200), new Point(580, 200)), ports.get(1));
    }

    @Test
    void collectsMultipleDestinationsOnTheSameNet() {
        Circuit circuit = new Circuit("Fan out", List.of(
                input("A", 0, new Point(100, 200)),
                output("Y", 0, new Point(340, 200)),
                output("Z", 0, new Point(340, 600))),
                List.of(), 1, List.of());

        assertEquals(List.of(new Point(100, 200), new Point(340, 200), new Point(340, 600)),
                collector.collect(circuit).get(0));
    }

    @Test
    void rejectsAnOutputWithNoDriver() {
        Circuit circuit = new Circuit("Undriven", List.of(
                output("Y", 0, new Point(340, 200))),
                List.of(), 1, List.of());

        assertThrows(IllegalArgumentException.class, () -> collector.collect(circuit));
    }

    @Test
    void rejectsMultipleDriversOnTheSameNet() {
        Circuit circuit = new Circuit("Conflicting drivers", List.of(
                input("A", 0, new Point(100, 200)),
                input("B", 0, new Point(100, 600)),
                output("Y", 0, new Point(340, 200))),
                List.of(), 1, List.of());

        assertThrows(IllegalArgumentException.class, () -> collector.collect(circuit));
    }

    private static InputPin input(String label, int net, Point location) {
        return new InputPin(label, label, net, location, Orientation.EAST);
    }

    private static OutputPin output(String label, int net, Point location) {
        return new OutputPin(label, label, net, location, Orientation.WEST);
    }
}
