package data_access.circ;

import entity.circuit.Orientation;
import entity.circuit.UnsupportedComponent;
import use_case.import_circuit.CircuitParseException;

import java.util.List;
import java.util.Optional;

/** Fallback for any component outside the supported subset. Its ports are unknown. */
final class UnsupportedDefinition implements ComponentDefinition {

    private final String reason;

    /** @param reason shown to the user after the standard warning text; may be empty */
    UnsupportedDefinition(String reason) {
        this.reason = reason;
    }

    @Override
    public PlacedComponent place(RawComponent raw) {
        Orientation facing = facingOrEast(raw);
        return new PlacedComponent(List.of(), Optional.empty(),
                (id, nets) -> new UnsupportedComponent(id, raw.name(), reason, raw.location(), facing));
    }

    /**
     * Gates can be configured to output only 0/floating or floating/1 ({@code out} attribute).
     * That needs bus resolution the simulator does not model, so such gates are unsupported.
     */
    static Optional<PlacedComponent> ifNonStandardOutput(RawComponent raw) {
        String out = raw.attribute("out", "01");
        if (out.equals("01")) {
            return Optional.empty();
        }
        return Optional.of(new UnsupportedDefinition(
                "Only the default output value setting (0/1) is supported.").place(raw));
    }

    private static Orientation facingOrEast(RawComponent raw) {
        try {
            return raw.facing(Orientation.EAST);
        } catch (CircuitParseException e) {
            return Orientation.EAST;
        }
    }
}
