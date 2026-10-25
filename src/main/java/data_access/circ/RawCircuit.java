package data_access.circ;

import java.util.List;

/** The main circuit of a .circ file exactly as written, before any interpretation. */
record RawCircuit(String name, List<RawComponent> components, List<RawWire> wires) {

    RawCircuit {
        components = List.copyOf(components);
        wires = List.copyOf(wires);
    }
}
