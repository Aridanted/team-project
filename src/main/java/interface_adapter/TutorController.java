package interface_adapter;

import use_case.TutorInputBoundary;
import use_case.TutorInputData;

/** Sends the student's message to the tutor use case. */
public class TutorController {
    private final TutorInputBoundary interactor;

    public TutorController(TutorInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void sendMessage(String message) {
        TutorInputData inputData = new TutorInputData(message);
        interactor.execute(inputData);
    }
}
