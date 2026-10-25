package data_access.circ;

import entity.circuit.CircuitComponent;
import entity.circuit.Point;

import java.util.List;

/** A domain component together with where its input ports are, for wiring checks. */
record Placement(CircuitComponent component, List<Point> inputPorts) {

    Placement {
        inputPorts = List.copyOf(inputPorts);
    }
}
