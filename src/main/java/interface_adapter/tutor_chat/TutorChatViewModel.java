package interface_adapter.tutor_chat;

import interface_adapter.ViewModel;

/** View model for the Tutor tab. */
public final class TutorChatViewModel extends ViewModel<TutorChatState> {

    public TutorChatViewModel() {
        super("tutor chat", new TutorChatState());
    }
}
