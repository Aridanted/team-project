package interface_adapter;

import use_case.tutor_chat.TutorChatInputBoundary;
import use_case.tutor_chat.TutorChatInputData;

/**
 * Controller for sending student chat messages and hint requests to the tutor chat interactor.
 */
public class TutorController {
    private final TutorChatInputBoundary interactor;

    /**
     * Constructs a TutorController.
     *
     * @param interactor the tutor chat input boundary
     */
    public TutorController(TutorChatInputBoundary interactor) {
        this.interactor = interactor;
    }

    /**
     * Sends a student message with default hint level 0.
     *
     * @param message the student's question or message
     */
    public void sendMessage(String message) {
        sendMessage(message, null, 0);
    }

    /**
     * Sends a student message with circuit check context and default hint level 0.
     *
     * @param message the student's question or message
     * @param lastCheckResult optional summary of the latest circuit check
     */
    public void sendMessage(String message, String lastCheckResult) {
        sendMessage(message, lastCheckResult, 0);
    }

    /**
     * Sends a student message with optional circuit check context and a hint level (0 to 3).
     *
     * @param message the student's question or message
     * @param lastCheckResult optional summary of the latest circuit check
     * @param hintLevel requested hint level (0 = normal, 1..3 = progressive hints)
     */
    public void sendMessage(String message, String lastCheckResult, int hintLevel) {
        final TutorChatInputData inputData = new TutorChatInputData(message, hintLevel);
        interactor.send(inputData);
    }
}
