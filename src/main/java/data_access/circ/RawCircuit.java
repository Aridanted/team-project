package data_access.circ;

import entity.circuit.Point;

import java.util.List;

/** The main circuit of a .circ file exactly as written, before any interpretation. */
record RawCircuit(String name, List<RawComponent> components, List<RawWire> wires) {

    /** A {@code <wire>} element: a straight segment between two grid points. */
    record RawWire(Point from, Point to) {
    }

    RawCircuit {
        components = List.copyOf(components);
        wires = List.copyOf(wires);
    }
}
