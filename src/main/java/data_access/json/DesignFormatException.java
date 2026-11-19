package data_access.json;

/** Thrown when text is not a valid circuit in the shared JSON format. The message says what is wrong. */
public class DesignFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    public DesignFormatException(String message) {
        super(message);
    }

    public DesignFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
