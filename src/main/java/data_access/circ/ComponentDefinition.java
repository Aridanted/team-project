package data_access.circ;

import use_case.import_circuit.CircuitParseException;

/**
 * Knows how one kind of Logisim component is laid out and what it means (Strategy). Adding
 * support for a component means adding a definition and registering it in
 * {@link ComponentRegistry}; the parser itself does not change.
 */
interface ComponentDefinition {

    PlacedComponent place(RawComponent raw) throws CircuitParseException;

    /** @throws CircuitParseException (E005) if the component is wider than one bit */
    static void requireSingleBit(RawComponent raw) throws CircuitParseException {
        int width = raw.width();
        if (width != 1) {
            throw Diagnostics.multiBit(raw.name(), raw.location(), width);
        }
    }
}
