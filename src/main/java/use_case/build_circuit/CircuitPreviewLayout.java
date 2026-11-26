package use_case.build_circuit;

import entity.circuit.Circuit;

/** Arranges circuit components and wires for the Builder preview. */
public interface CircuitPreviewLayout {

    /**
     * Creates a circuit drawing while preserving its logical behaviour.
     *
     * @param circuit the circuit to arrange
     * @return the positioned circuit with drawing wires
     * @throws IllegalArgumentException if the circuit cannot be arranged
     */
    Circuit layout(Circuit circuit);
}
