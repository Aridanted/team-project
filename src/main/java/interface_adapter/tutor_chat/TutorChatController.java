package interface_adapter.tutor_chat;

import java.util.Objects;

import use_case.tutor_chat.TutorChatInputBoundary;
import use_case.tutor_chat.TutorChatInputData;

/** Called by the Tutor tab. {@link #send} waits for the AI, so call it off the event thread. */
public final class TutorChatController {

    private final TutorChatInputBoundary interactor;

    /**
     * Creates a controller that forwards the tutor tab's requests to the interactor.
     *
     * @param interactor the tutor chat use case
     */
    public TutorChatController(TutorChatInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor, "interactor");
    }

    /**
     * Sends a normal chat message (hint level 0).
     *
     * @param message what the student typed
     */
    public void send(String message) {
        send(message, 0);
    }

    /**
     * Sends a message at the given hint level.
     *
     * @param message what the student typed
     * @param hintLevel 0 for normal chat, 1 to 3 for the Hint 1 / 2 / 3 buttons
     */
    public void send(String message, int hintLevel) {
        interactor.send(new TutorChatInputData(message, hintLevel));
    }

    /** Forgets the conversation. */
    public void clear() {
        interactor.clear();
    }
}
