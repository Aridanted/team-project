package interface_adapter.build_circuit;

import interface_adapter.import_circuit.DrawableComponent;
import interface_adapter.import_circuit.DrawableWire;

import java.util.List;

/**
 * The latest result shown by the Builder.
 *
 * @param circuitName the name of the built circuit
 * @param truthTableText the simulated truth table formatted for display
 * @param error the message to display when a build fails
 * @param explanation the explanation accompanying the circuit
 * @param components the components shown in the circuit preview
 * @param wires the wires shown in the circuit preview
 */
public record BuildCircuitState(
        String circuitName,
        String truthTableText,
        String error,
        String explanation,
        List<DrawableComponent> components,
        List<DrawableWire> wires) {

    /**
     * Copies the drawing lists to keep the state immutable.
     */
    public BuildCircuitState {
        components = List.copyOf(components);
        wires = List.copyOf(wires);
    }

    /**
     * Creates Builder state without drawing data.
     *
     * @param circuitName the name of the built circuit
     * @param truthTableText the formatted truth table
     * @param error the error message
     * @param explanation the explanation accompanying the circuit
     */
    public BuildCircuitState(
            String circuitName,
            String truthTableText,
            String error,
            String explanation) {
        this(circuitName, truthTableText, error, explanation, List.of(), List.of());
    }

    /**
     * Creates Builder state without an explanation or drawing data.
     *
     * @param circuitName the name of the built circuit
     * @param truthTableText the formatted truth table
     * @param error the error message
     */
    public BuildCircuitState(
            String circuitName,
            String truthTableText,
            String error) {
        this(circuitName, truthTableText, error, "");
    }
}
