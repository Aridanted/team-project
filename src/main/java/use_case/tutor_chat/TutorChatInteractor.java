package use_case.tutor_chat;

import java.util.Objects;

import entity.tutor.ChatMessage;
import entity.tutor.ChatRole;
import entity.tutor.Conversation;

/**
 * Validates the student's message, asks the tutor for a reply with the open circuit and its last
 * check as context, filters the reply so it never gives the answer away, and saves the conversation.
 * Falling back to the offline tutor is the gateway's job (see {@code FallbackTutorGateway}).
 */
public final class TutorChatInteractor implements TutorChatInputBoundary {

    public static final int MAX_MESSAGE_LENGTH = 500;

    private final TutorChatDataAccessInterface session;
    private final TutorGateway tutor;
    private final TutorChatOutputBoundary presenter;
    private final TutorSafetyFilter safetyFilter;

    /**
     * Constructs a TutorChatInteractor with the default safety filter.
     *
     * @param session the tutor session data access
     * @param tutor the tutor gateway
     * @param presenter the output boundary presenter
     */
    public TutorChatInteractor(
            TutorChatDataAccessInterface session,
            TutorGateway tutor,
            TutorChatOutputBoundary presenter
    ) {
        this(session, tutor, presenter, new TutorSafetyFilter());
    }

    /**
     * Constructs a TutorChatInteractor with a custom safety filter.
     *
     * @param session the tutor session data access
     * @param tutor the tutor gateway
     * @param presenter the output boundary presenter
     * @param safetyFilter the safety filter for tutor replies
     */
    public TutorChatInteractor(
            TutorChatDataAccessInterface session,
            TutorGateway tutor,
            TutorChatOutputBoundary presenter,
            TutorSafetyFilter safetyFilter
    ) {
        this.session = Objects.requireNonNull(session, "session");
        this.tutor = Objects.requireNonNull(tutor, "tutor");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
        this.safetyFilter = Objects.requireNonNull(safetyFilter, "safetyFilter");
    }

    @Override
    public void send(TutorChatInputData inputData) {
        final String message = validateAndExtractMessage(inputData);
        if (message == null) {
            return;
        }

        final Conversation baseConversation = session.getConversation();
        final Conversation withQuestion = baseConversation.append(ChatMessage.fromStudent(message));
        final Conversation conversationForRequest = buildConversationForRequest(
                baseConversation, withQuestion, message, inputData.hintLevel()
        );

        final TutorReply answer;
        try {
            answer = tutor.answer(new TutorRequest(conversationForRequest, session.getCurrentCircuit(),
                    session.getLastCheck(), inputData.hintLevel()));
        } catch (TutorUnavailableException ex) {
            presenter.prepareFailView("The tutor could not answer: " + ex.getMessage());
            return;
        }
        final String reply = answer == null ? null : answer.text();
        if (reply == null || reply.isBlank()) {
            presenter.prepareFailView("The tutor could not answer: the reply was empty.");
            return;
        }

        final String safeReply = safetyFilter.filter(reply.strip());
        final ChatMessage tutorMessage = answer.offline()
                ? ChatMessage.fromOfflineTutor(safeReply)
                : ChatMessage.fromTutor(safeReply);
        final Conversation withReply = withQuestion.append(tutorMessage);
        session.saveConversation(withReply);
        presenter.prepareSuccessView(toOutputData(withReply));
    }

    @Override
    public void clear() {
        session.saveConversation(Conversation.empty());
        presenter.prepareSuccessView(toOutputData(Conversation.empty()));
    }

    private String validateAndExtractMessage(TutorChatInputData inputData) {
        final String message = inputData == null || inputData.message() == null
                ? ""
                : inputData.message().strip();
        if (message.isEmpty()) {
            presenter.prepareFailView("Message cannot be empty!");
            return null;
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            presenter.prepareFailView("Message is too long (" + message.length()
                    + " characters, at most " + MAX_MESSAGE_LENGTH + ").");
            return null;
        }
        return message;
    }

    private Conversation buildConversationForRequest(
            Conversation baseConversation,
            Conversation withQuestion,
            String message,
            int hintLevel
    ) {
        if (hintLevel <= 0) {
            return withQuestion;
        }
        final String promptForTutor = HintPromptBuilder.buildPrompt(message, hintLevel);
        return baseConversation.append(ChatMessage.fromStudent(promptForTutor));
    }

    private static TutorChatOutputData toOutputData(Conversation conversation) {
        return new TutorChatOutputData(conversation.messages().stream()
                .map(message -> new TutorChatOutputData.ChatLine(
                        message.role() == ChatRole.STUDENT, message.text(), message.offline()))
                .toList());
    }
}
