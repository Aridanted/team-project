package use_case;

/** Receives the tutor's reply, or the reason there is none. */
public interface TutorOutputBoundary {
    void prepareSuccessView(String response);
    void prepareFailView(String error);
}
