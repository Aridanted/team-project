package use_case.tutor_chat;

import java.util.Objects;

import entity.tutor.ChatMessage;
import entity.tutor.ChatRole;
import entity.tutor.Conversation;
import use_case.RuleBasedTutor;
import use_case.TutorPromptBuilder;
import use_case.TutorSafetyFilter;

/**
 * Validates the student's message, asks the tutor for a reply with the open circuit and its last
 * check as context, applies hint formatting and safety filtering with offline fallback, and saves
 * the conversation.
 */
public final class TutorChatInteractor implements TutorChatInputBoundary {

    public static final int MAX_MESSAGE_LENGTH = 500;

    private final TutorChatDataAccessInterface session;
    private final TutorGateway tutor;
    private final TutorChatOutputBoundary presenter;
    private final TutorSafetyFilter safetyFilter;
    private final RuleBasedTutor offlineTutor;

    /**
     * Constructs a TutorChatInteractor with default safety filter and offline rule-based tutor.
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
        this(session, tutor, presenter, new TutorSafetyFilter(), new RuleBasedTutor());
    }

    /**
     * Constructs a TutorChatInteractor with custom safety filter and offline rule-based tutor.
     *
     * @param session the tutor session data access
     * @param tutor the tutor gateway
     * @param presenter the output boundary presenter
     * @param safetyFilter the safety filter for tutor replies
     * @param offlineTutor the offline rule-based fallback tutor
     */
    public TutorChatInteractor(
            TutorChatDataAccessInterface session,
            TutorGateway tutor,
            TutorChatOutputBoundary presenter,
            TutorSafetyFilter safetyFilter,
            RuleBasedTutor offlineTutor
    ) {
        this.session = Objects.requireNonNull(session, "session");
        this.tutor = Objects.requireNonNull(tutor, "tutor");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
        this.safetyFilter = safetyFilter != null ? safetyFilter : new TutorSafetyFilter();
        this.offlineTutor = offlineTutor != null ? offlineTutor : new RuleBasedTutor();
    }

    @Override
    public void send(TutorChatInputData inputData) {
        final String rawMessage = validateAndExtractMessage(inputData);
        if (rawMessage == null) {
            return;
        }

        final int hintLevel = inputData != null ? inputData.hintLevel() : 0;
        final Conversation baseConversation = session.getConversation();
        final Conversation withQuestion = baseConversation.append(ChatMessage.fromStudent(rawMessage));
        final Conversation conversationForRequest = buildConversationForRequest(
                baseConversation, withQuestion, rawMessage, hintLevel
        );

        final ReplyResult replyResult = fetchReplyWithFallback(conversationForRequest, rawMessage);
        final String filteredReply = safetyFilter.filter(replyResult.text().strip());
        if (filteredReply == null || filteredReply.isBlank()) {
            presenter.prepareFailView("The tutor could not answer: the reply was empty.");
            return;
        }

        final String finalReply = replyResult.offline() ? "[Offline] " + filteredReply : filteredReply;
        final Conversation withReply = withQuestion.append(ChatMessage.fromTutor(finalReply.strip()));
        session.saveConversation(withReply);
        presenter.prepareSuccessView(toOutputData(withReply));
    }

    @Override
    public void clear() {
        session.saveConversation(Conversation.empty());
        presenter.prepareSuccessView(toOutputData(Conversation.empty()));
    }

    private String validateAndExtractMessage(TutorChatInputData inputData) {
        final String rawMessage = inputData == null || inputData.message() == null
                ? ""
                : inputData.message().strip();
        if (rawMessage.isEmpty()) {
            presenter.prepareFailView("Message cannot be empty!");
            return null;
        }
        if (rawMessage.length() > MAX_MESSAGE_LENGTH) {
            presenter.prepareFailView("Message is too long (" + rawMessage.length()
                    + " characters, at most " + MAX_MESSAGE_LENGTH + ").");
            return null;
        }
        return rawMessage;
    }

    private Conversation buildConversationForRequest(
            Conversation baseConversation,
            Conversation withQuestion,
            String rawMessage,
            int hintLevel
    ) {
        if (hintLevel <= 0) {
            return withQuestion;
        }
        final String promptForTutor = TutorPromptBuilder.buildPrompt(rawMessage, hintLevel);
        return baseConversation.append(ChatMessage.fromStudent(promptForTutor));
    }

    private ReplyResult fetchReplyWithFallback(Conversation conversationForRequest, String rawMessage) {
        try {
            final String aiReply = tutor.reply(
                    new TutorRequest(conversationForRequest, session.getCurrentCircuit(), session.getLastCheck())
            );
            if (aiReply != null && !aiReply.isBlank()) {
                return new ReplyResult(aiReply, false);
            }
        } catch (TutorUnavailableException ignored) {
            // Fall back to rule-based offline tutor when primary gateway is unavailable.
        }
        return new ReplyResult(
                offlineTutor.generateOfflineResponse(rawMessage, formatLastCheck()),
                true
        );
    }

    private String formatLastCheck() {
        return session.getLastCheck() == null ? null : session.getLastCheck().toString();
    }

    private static TutorChatOutputData toOutputData(Conversation conversation) {
        return new TutorChatOutputData(conversation.messages().stream()
                .map(message -> new TutorChatOutputData.ChatLine(
                        message.role() == ChatRole.STUDENT, message.text()))
                .toList());
    }

    private record ReplyResult(String text, boolean offline) {
    }
}
