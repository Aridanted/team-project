package use_case;

public interface TutorOutputBoundary {
    void prepareSuccessView(String response);
    void prepareFailView(String error);
}