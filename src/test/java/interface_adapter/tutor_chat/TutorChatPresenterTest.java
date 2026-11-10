package interface_adapter.tutor_chat;

import org.junit.jupiter.api.Test;
import use_case.tutor_chat.TutorChatOutputData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TutorChatPresenterTest {

    private final TutorChatViewModel viewModel = new TutorChatViewModel();
    private final TutorChatPresenter presenter = new TutorChatPresenter(viewModel);

    @Test
    void showsConversationAsBubblesAndClearsError() {
        presenter.prepareFailView("old error");

        presenter.prepareSuccessView(new TutorChatOutputData(List.of(
                new TutorChatOutputData.ChatLine(true, "Why?"),
                new TutorChatOutputData.ChatLine(false, "Look at row 3."))));

        assertEquals(List.of(new ChatBubble(true, "You", "Why?"), new ChatBubble(false, "Tutor", "Look at row 3.")),
                viewModel.getState().getMessages());
        assertEquals("", viewModel.getState().getError());
    }

    @Test
    void failureKeepsMessages() {
        presenter.prepareSuccessView(new TutorChatOutputData(List.of(new TutorChatOutputData.ChatLine(true, "Hi"))));

        presenter.prepareFailView("The tutor could not answer: offline");

        assertEquals(1, viewModel.getState().getMessages().size());
        assertEquals("The tutor could not answer: offline", viewModel.getState().getError());
    }
}
