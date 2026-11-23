package data_access.circ;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.InputPin;
import entity.circuit.Orientation;
import entity.circuit.OutputPin;
import entity.circuit.Point;
import entity.circuit.GateInput;
import entity.circuit.GateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Positions circuit components in columns according to their signal depth. */
public final class CircuitLayout {
    private static final int LEFT_MARGIN = 100;
    private static final int TOP_MARGIN = 200;
    private static final int COLUMN_SPACING = 240;
    private static final int ROW_SPACING = 400;

    private final GateDepthCalculator depthCalculator;

    /**
     * Creates a layout using the supplied depth calculator.
     *
     * @param depthCalculator the calculator used to assign gate columns
     */
    public CircuitLayout(GateDepthCalculator depthCalculator) {
        this.depthCalculator = Objects.requireNonNull(depthCalculator, "depthCalculator");
    }

    /**
     * Positions the components while preserving their IDs, nets, and logic.
     *
     * @param circuit the circuit to arrange
     * @return a positioned circuit ready for wire routing
     * @throws IllegalArgumentException if the circuit cannot be laid out
     */
    public Circuit layout(Circuit circuit) {
        Map<String, Integer> depths = depthCalculator.calculate(circuit);
        int outputColumn = depths.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        Map<Integer, Integer> nextRowByColumn = new HashMap<>();
        List<CircuitComponent> positioned = new ArrayList<>();

        for (CircuitComponent component : circuit.components()) {
            int column = columnFor(component, depths, outputColumn);
            int row = nextRowByColumn.getOrDefault(column, 0);
            nextRowByColumn.put(column, row + 1);
            Point location = new Point(LEFT_MARGIN + column * COLUMN_SPACING,
                    TOP_MARGIN + row * ROW_SPACING);
            positioned.add(move(component, location));
        }

        return new Circuit(circuit.name(), positioned, List.of(),
                circuit.netCount(), circuit.warnings());
    }

    private int columnFor(CircuitComponent component, Map<String, Integer> depths, int outputColumn) {
        if (component instanceof Gate gate) {
            return depths.get(gate.id());
        }
        if (component instanceof OutputPin) {
            return outputColumn;
        }
        return 0;
    }

    private CircuitComponent move(CircuitComponent component, Point location) {
        return switch (component) {
            case InputPin pin -> new InputPin(pin.id(), pin.label(), pin.net(),
                    location, Orientation.EAST);
            case OutputPin pin -> new OutputPin(pin.id(), pin.label(), pin.net(),
                    location, Orientation.WEST);
            case Constant constant -> new Constant(constant.id(), constant.value(), constant.net(),
                    location, Orientation.EAST);
            case Gate gate -> moveGate(gate, location);
            default -> throw new IllegalArgumentException(
                    "Cannot position unsupported component " + component.id());
        };
    }

    private Gate moveGate(Gate gate, Point location) {
        GateType type = gate.type();
        List<GateInput> inputs = gate.inputs();

        if (type.isUnary() && inputs.getFirst().negated()) {
            type = type == GateType.NOT ? GateType.BUFFER : GateType.NOT;
            inputs = List.of(new GateInput(inputs.getFirst().net(), false));
        }

        return new Gate(gate.id(), type, inputs, gate.outputNet(),
                location, Orientation.EAST);
    }
}
