package data_access.circ;

import entity.circuit.CircuitComponent;
import entity.circuit.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A component whose ports have been located but whose nets are not known yet. Once nets are
 * built, {@link #factory()} turns it into the domain entity.
 *
 * @param inputPorts ports the component reads, in port order
 * @param outputPort port the component drives, if any
 */
record PlacedComponent(List<Point> inputPorts, Optional<Point> outputPort, EntityFactory factory) {

    /** Creates the domain entity once every port has a net. */
    @FunctionalInterface
    interface EntityFactory {
        CircuitComponent create(String id, NetMap nets);
    }

    PlacedComponent {
        inputPorts = List.copyOf(inputPorts);
    }

    List<Point> ports() {
        List<Point> ports = new ArrayList<>(inputPorts);
        outputPort.ifPresent(ports::add);
        return ports;
    }
}
