package use_case.import_circuit;

import java.util.Objects;

/** Thrown when a circuit file cannot be turned into a circuit. The message is shown to the user. */
public class CircuitParseException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String code;

    /**
     * @param code    stable identifier such as "E001", for tests and documentation
     * @param message human-readable explanation
     */
    public CircuitParseException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public String code() {
        return code;
    }
}
