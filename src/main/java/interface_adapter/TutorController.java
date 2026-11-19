package interface_adapter;

import use_case.TutorInputBoundary;
import use_case.TutorInputData;

public class TutorController {
    private final TutorInputBoundary interactor;

    public void sendMessage(String message) {
        sendMessage(message, null);
    }

    public TutorController(TutorInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void sendMessage(String message, String lastCheckResult) {
        interactor.execute(new TutorInputData(message, lastCheckResult));
    }
}