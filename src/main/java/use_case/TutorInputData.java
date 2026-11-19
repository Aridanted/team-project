package use_case;

public class TutorInputData {
    private final String message;
    private final String lastCheckResult;

    public TutorInputData(String message, String lastCheckResult) {
        this.message = message;
        this.lastCheckResult = lastCheckResult;
    }

    public String getMessage() {
        return message;
    }

    public String getLastCheckResult() {
        return lastCheckResult;
    }

    public TutorInputData(String message) {
        this(message, null);
    }
}