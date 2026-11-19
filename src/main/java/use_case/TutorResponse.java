package use_case;

public class TutorResponse {
    private final String message;
    private final boolean isOffline;

    public TutorResponse(String message, boolean isOffline) {
        this.message = message;
        this.isOffline = isOffline;
    }

    public String getMessage() {
        return message;
    }

    public boolean isOffline() {
        return isOffline;
    }
}