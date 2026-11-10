package data_access.circ;

import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.GateType;
import entity.circuit.Orientation;
import entity.circuit.Point;
import use_case.import_circuit.CircuitParseException;

import java.util.List;
import java.util.Optional;

/** Logisim's single-input gates: NOT Gate (size 20 or 30) and Buffer (fixed width). */
final class UnaryGateDefinition implements ComponentDefinition {

    private final GateType type;
    private final int defaultWidth;
    private final boolean widthFromSizeAttribute;

    /**
     * @param defaultWidth           distance from the input to the output point
     * @param widthFromSizeAttribute whether the {@code size} attribute overrides that width
     */
    UnaryGateDefinition(GateType type, int defaultWidth, boolean widthFromSizeAttribute) {
        this.type = type;
        this.defaultWidth = defaultWidth;
        this.widthFromSizeAttribute = widthFromSizeAttribute;
    }

    @Override
    public PlacedComponent place(RawComponent raw) throws CircuitParseException {
        ComponentDefinition.requireSingleBit(raw);
        Optional<PlacedComponent> unsupported = UnsupportedDefinition.ifNonStandardOutput(raw);
        if (unsupported.isPresent()) {
            return unsupported.get();
        }
        int width = widthFromSizeAttribute ? raw.intAttribute("size", defaultWidth) : defaultWidth;
        Orientation facing = raw.facing(Orientation.EAST);
        Point output = raw.location();
        Point input = GateGeometry.place(output, -width, 0, facing);
        return new PlacedComponent(List.of(input), Optional.of(output), (id, nets) -> new Gate(id, type,
                List.of(new GateInput(nets.netAt(input), false)), nets.netAt(output), output, facing));
    }
}
