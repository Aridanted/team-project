package entity.simulation;

/**
 * Thrown when a circuit cannot be simulated: it contains unsupported components, has a feedback
 * loop, or has too many inputs to enumerate. The message is shown to the user.
 */
public class NotSimulatableException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotSimulatableException(String message) {
        super(message);
    }
}
