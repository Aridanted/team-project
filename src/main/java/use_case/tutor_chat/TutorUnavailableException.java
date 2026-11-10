package use_case.tutor_chat;

/** The tutor could not answer. The message is safe to show the student. */
public class TutorUnavailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public TutorUnavailableException(String message) {
        super(message);
    }

    public TutorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
