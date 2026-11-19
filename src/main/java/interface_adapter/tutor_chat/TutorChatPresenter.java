package interface_adapter.tutor_chat;

import java.util.Objects;

import use_case.tutor_chat.TutorChatOutputBoundary;
import use_case.tutor_chat.TutorChatOutputData;

/** Turns the conversation into chat bubbles for {@link TutorChatState}. */
public final class TutorChatPresenter implements TutorChatOutputBoundary {

    static final String STUDENT = "You";
    static final String TUTOR = "Tutor";

    private final TutorChatViewModel viewModel;

    public TutorChatPresenter(TutorChatViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
    }

    @Override
    public void prepareSuccessView(TutorChatOutputData outputData) {
        final TutorChatState state = new TutorChatState(viewModel.getState());
        state.setMessages(outputData.messages().stream()
                .map(line -> new ChatBubble(line.fromStudent(), line.fromStudent() ? STUDENT : TUTOR, line.text()))
                .toList());
        state.setError("");
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        final TutorChatState state = new TutorChatState(viewModel.getState());
        state.setError(errorMessage);
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }
}
