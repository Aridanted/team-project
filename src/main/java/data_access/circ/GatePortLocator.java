package data_access.circ;

import entity.circuit.Gate;
import entity.circuit.GateInput;
import entity.circuit.GateType;
import entity.circuit.Point;

import java.util.ArrayList;
import java.util.List;

/** Locates gate input ports using the dimensions of exported Logisim gates. */
public final class GatePortLocator {
    static final int NOT_WIDTH = 30;
    static final int BUFFER_WIDTH = 20;

    /**
     * Locates input ports in the same order as the gate's inputs.
     *
     * @param gate the positioned gate
     * @return the input port coordinates
     * @throws IllegalArgumentException if a unary gate still has a negated input
     */
    public List<Point> inputPorts(Gate gate) {
        if (gate.type().isUnary()) {
            return List.of(unaryInput(gate));
        }

        List<Point> ports = new ArrayList<>();
        int inputCount = gate.inputs().size();
        for (int index = 0; index < inputCount; index++) {
            GateInput input = gate.inputs().get(index);
            int bubbleWidth = input.negated() ? GateGeometry.NEGATION_BUBBLE : 0;
            int width = GateGeometry.DEFAULT_SIZE + extraWidth(gate.type()) + bubbleWidth;
            int offset = GateGeometry.inputOffset(GateGeometry.DEFAULT_SIZE, inputCount, index);
            ports.add(GateGeometry.place(gate.location(), -width, offset, gate.facing()));
        }
        return List.copyOf(ports);
    }

    private Point unaryInput(Gate gate) {
        if (gate.inputs().getFirst().negated()) {
            throw new IllegalArgumentException(
                    "Normalize the negated input of unary gate " + gate.id() + " before export.");
        }

        int width = gate.type() == GateType.NOT ? NOT_WIDTH : BUFFER_WIDTH;
        return GateGeometry.place(gate.location(), -width, 0, gate.facing());
    }

    private int extraWidth(GateType type) {
        return switch (type) {
            case NAND, NOR, XOR, XOR_ONE -> 10;
            case XNOR, XNOR_ONE -> 20;
            default -> 0;
        };
    }
}
