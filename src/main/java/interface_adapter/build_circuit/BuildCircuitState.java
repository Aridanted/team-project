package interface_adapter.build_circuit;

/**
 * The latest result shown by the Builder.
 *
 * @param circuitName the name of the built circuit
 * @param truthTableText the simulated truth table formatted for display
 * @param error the message to display when a build fails
 */
public record BuildCircuitState(String circuitName, String truthTableText, String error) {
}
