package data_access.circ;

import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.GateType;
import entity.circuit.Orientation;
import entity.circuit.Point;
import use_case.import_circuit.CircuitParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Logisim's multi-input gates: AND, OR, NAND, NOR, XOR and XNOR. */
final class GateDefinition implements ComponentDefinition {

    private final GateType type;
    private final GateType exactlyOneType;
    private final int extraWidth;

    private GateDefinition(GateType type, GateType exactlyOneType, int extraWidth) {
        this.type = type;
        this.exactlyOneType = exactlyOneType;
        this.extraWidth = extraWidth;
    }

    /**
     * @param extraWidth how much longer than its {@code size} the gate is (for the output bubble
     *                   or the XOR's curved back)
     */
    static GateDefinition standard(GateType type, int extraWidth) {
        return new GateDefinition(type, type, extraWidth);
    }

    /**
     * XOR and XNOR have a {@code xor} attribute: {@code "odd"} means parity, {@code "1"} (the
     * default) means exactly one input is one. The two agree for two inputs.
     */
    static GateDefinition xorFamily(GateType parityType, GateType exactlyOneType, int extraWidth) {
        return new GateDefinition(parityType, exactlyOneType, extraWidth);
    }

    @Override
    public PlacedComponent place(RawComponent raw) throws CircuitParseException {
        ComponentDefinition.requireSingleBit(raw);
        Optional<PlacedComponent> unsupported = UnsupportedDefinition.ifNonStandardOutput(raw);
        if (unsupported.isPresent()) {
            return unsupported.get();
        }
        int size = raw.intAttribute("size", GateGeometry.DEFAULT_SIZE);
        int inputCount = raw.intAttribute("inputs", GateGeometry.DEFAULT_INPUTS);
        if (inputCount < 1) {
            throw Diagnostics.malformed("input count " + inputCount + " of " + raw.name() + " at " + raw.location());
        }
        Orientation facing = raw.facing(Orientation.EAST);
        GateType gateType = inputCount > 2 && !"odd".equals(raw.attribute("xor", "1")) ? exactlyOneType : type;

        Point output = raw.location();
        List<Point> ports = new ArrayList<>();
        List<Boolean> negated = new ArrayList<>();
        for (int i = 0; i < inputCount; i++) {
            boolean isNegated = raw.booleanAttribute("negate" + i);
            int dx = -(size + extraWidth + (isNegated ? GateGeometry.NEGATION_BUBBLE : 0));
            ports.add(GateGeometry.place(output, dx, GateGeometry.inputOffset(size, inputCount, i), facing));
            negated.add(isNegated);
        }
        return new PlacedComponent(ports, Optional.of(output), (id, nets) -> {
            List<GateInput> inputs = new ArrayList<>();
            for (int i = 0; i < ports.size(); i++) {
                inputs.add(new GateInput(nets.netAt(ports.get(i)), negated.get(i)));
            }
            return new Gate(id, gateType, inputs, nets.netAt(output), output, facing);
        });
    }
}
