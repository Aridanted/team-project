package use_case.save_built_circuit;

/** Reports a failure to write a built circuit. */
public class CircuitFileWriteException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a file-writing failure.
     *
     * @param message the failure message
     * @param cause the underlying failure
     */
    public CircuitFileWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
