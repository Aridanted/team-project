package interface_adapter.build_circuit;

/**
 * The latest result shown by the Builder.
 *
 * @param circuitName the name of the built circuit
 * @param truthTableText the simulated truth table formatted for display
 * @param error the message to display when a build fails
 * @param explanation the explanation accompanying the circuit
 */
public record BuildCircuitState(String circuitName, String truthTableText, String error,
                                String explanation) {

    /**
     * Creates Builder state without an explanation.
     *
     * @param circuitName the name of the built circuit
     * @param truthTableText the formatted truth table
     * @param error the error message
     */
    public BuildCircuitState(String circuitName, String truthTableText, String error) {
        this(circuitName, truthTableText, error, "");
    }
}
