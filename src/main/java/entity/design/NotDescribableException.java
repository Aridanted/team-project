package entity.design;

/** Thrown when a circuit cannot be written as a {@link CircuitDesign}. The message is for the user. */
public class NotDescribableException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotDescribableException(String message) {
        super(message);
    }
}
