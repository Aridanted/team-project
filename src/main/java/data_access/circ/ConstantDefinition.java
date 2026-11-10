package data_access.circ;

import entity.circuit.Constant;
import entity.circuit.Orientation;
import entity.circuit.Point;
import use_case.import_circuit.CircuitParseException;

import java.util.List;
import java.util.Optional;

/** Logisim's Constant (value from its {@code value} attribute), Power (1) and Ground (0). */
final class ConstantDefinition implements ComponentDefinition {

    @FunctionalInterface
    private interface ValueSource {
        boolean valueOf(RawComponent raw) throws CircuitParseException;
    }

    private final ValueSource valueSource;
    private final Orientation defaultFacing;

    private ConstantDefinition(ValueSource valueSource, Orientation defaultFacing) {
        this.valueSource = valueSource;
        this.defaultFacing = defaultFacing;
    }

    /** Wiring &gt; Constant: value attribute such as {@code 0x1}, default 1, facing east. */
    static ConstantDefinition fromValueAttribute() {
        return new ConstantDefinition(ConstantDefinition::parseValue, Orientation.EAST);
    }

    /** Wiring &gt; Power or Ground: always the given value. */
    static ConstantDefinition fixed(boolean value, Orientation defaultFacing) {
        return new ConstantDefinition(raw -> value, defaultFacing);
    }

    @Override
    public PlacedComponent place(RawComponent raw) throws CircuitParseException {
        ComponentDefinition.requireSingleBit(raw);
        Point at = raw.location();
        Orientation facing = raw.facing(defaultFacing);
        boolean value = valueSource.valueOf(raw);
        return new PlacedComponent(List.of(), Optional.of(at),
                (id, nets) -> new Constant(id, value, nets.netAt(at), at, facing));
    }

    private static boolean parseValue(RawComponent raw) throws CircuitParseException {
        String text = raw.attribute("value", "0x1").trim();
        try {
            long value = text.startsWith("0x") || text.startsWith("0X")
                    ? Long.parseLong(text.substring(2), 16)
                    : Long.parseLong(text);
            return value != 0;
        } catch (NumberFormatException e) {
            throw Diagnostics.malformed("constant value \"" + text + "\" at " + raw.location());
        }
    }
}
