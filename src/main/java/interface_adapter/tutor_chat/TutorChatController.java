package interface_adapter.tutor_chat;

import use_case.tutor_chat.TutorChatInputBoundary;
import use_case.tutor_chat.TutorChatInputData;

import java.util.Objects;

/** Called by the Tutor tab. {@link #send} waits for the AI, so call it off the event thread. */
public final class TutorChatController {

    private final TutorChatInputBoundary interactor;

    public TutorChatController(TutorChatInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor, "interactor");
    }

    public void send(String message) {
        interactor.send(new TutorChatInputData(message));
    }

    public void clear() {
        interactor.clear();
    }
}
