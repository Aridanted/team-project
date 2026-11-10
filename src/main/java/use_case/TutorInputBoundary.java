package use_case;

/** Sends a student's message to the tutor. */
public interface TutorInputBoundary {
    void execute(TutorInputData inputData);
}
