package use_case;

public class TutorInputData {
    private final String message;
    private final String lastCheckResult;
    private final int hintLevel;

    public TutorInputData(String message, String lastCheckResult, int hintLevel) {
        this.message = message;
        this.lastCheckResult = lastCheckResult;
        this.hintLevel = hintLevel;
    }

    public TutorInputData(String message, String lastCheckResult) {
        this(message, lastCheckResult, 0);
    }

    public TutorInputData(String message) {
        this(message, null, 0);
    }

    public String getMessage() {
        return message;
    }

    public String getLastCheckResult() {
        return lastCheckResult;
    }

    public int getHintLevel() {
        return hintLevel;
    }
}