package use_case.build_circuit;

import entity.circuit.Circuit;
import entity.design.CircuitDesign;
import entity.truthtable.TruthTable;

/**
 * The result of building and verifying a circuit.
 *
 * @param design the validated design
 * @param circuit the assembled circuit
 * @param truthTable the simulated truth table
 */
public record BuildCircuitOutputData(CircuitDesign design, Circuit circuit, TruthTable truthTable) {
}
