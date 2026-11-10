package use_case;

/** The message the student typed to the tutor. */
public class TutorInputData {
    private final String message;

    public TutorInputData(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
