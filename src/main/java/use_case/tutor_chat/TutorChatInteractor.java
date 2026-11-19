package use_case.tutor_chat;

import entity.tutor.ChatMessage;
import entity.tutor.ChatRole;
import entity.tutor.Conversation;

import java.util.Objects;

/**
 * Validates the student's message, asks the tutor for a reply with the open circuit and its last
 * check as context, saves both messages to the conversation, and presents it.
 */
public final class TutorChatInteractor implements TutorChatInputBoundary {

    public static final int MAX_MESSAGE_LENGTH = 500;

    private final TutorChatDataAccessInterface session;
    private final TutorGateway tutor;
    private final TutorChatOutputBoundary presenter;

    public TutorChatInteractor(TutorChatDataAccessInterface session, TutorGateway tutor,
                               TutorChatOutputBoundary presenter) {
        this.session = Objects.requireNonNull(session, "session");
        this.tutor = Objects.requireNonNull(tutor, "tutor");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    @Override
    public void send(TutorChatInputData inputData) {
        String message = inputData.message() == null ? "" : inputData.message().strip();
        if (message.isEmpty()) {
            presenter.prepareFailView("Message cannot be empty!");
            return;
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            presenter.prepareFailView("Message is too long (" + message.length() + " characters, at most "
                    + MAX_MESSAGE_LENGTH + ").");
            return;
        }

        Conversation withQuestion = session.getConversation().append(ChatMessage.fromStudent(message));
        String reply;
        try {
            reply = tutor.reply(new TutorRequest(withQuestion, session.getCurrentCircuit(), session.getLastCheck()));
        } catch (TutorUnavailableException e) {
            presenter.prepareFailView("The tutor could not answer: " + e.getMessage());
            return;
        }
        if (reply == null || reply.isBlank()) {
            presenter.prepareFailView("The tutor could not answer: the reply was empty.");
            return;
        }

        Conversation withReply = withQuestion.append(ChatMessage.fromTutor(reply.strip()));
        session.saveConversation(withReply);
        presenter.prepareSuccessView(toOutputData(withReply));
    }

    @Override
    public void clear() {
        session.saveConversation(Conversation.empty());
        presenter.prepareSuccessView(toOutputData(Conversation.empty()));
    }

    private static TutorChatOutputData toOutputData(Conversation conversation) {
        return new TutorChatOutputData(conversation.messages().stream()
                .map(message -> new TutorChatOutputData.ChatLine(
                        message.role() == ChatRole.STUDENT, message.text()))
                .toList());
    }
}
