package use_case.build_circuit;

/** Reports a failure to generate a circuit design. */
public class DesignGenerationException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a failure with a message describing what went wrong.
     *
     * @param message the failure message
     */
    public DesignGenerationException(String message) {
        super(message);
    }
}
