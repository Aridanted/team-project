package use_case;

import java.util.List;

import entity.tutor.ChatMessage;
import entity.tutor.Conversation;
import use_case.tutor_chat.TutorGateway;
import use_case.tutor_chat.TutorRequest;
import use_case.tutor_chat.TutorUnavailableException;

/**
 * Gateway that tries a primary AI tutor gateway first and falls back to the offline rule-based tutor.
 */
public class FallbackTutorGateway implements TutorGateway {
    private final TutorGateway primaryAi;
    private final RuleBasedTutor offlineTutor;

    /**
     * Constructs a FallbackTutorGateway.
     *
     * @param primaryAi the primary AI tutor gateway
     * @param offlineTutor the offline rule-based fallback tutor
     */
    public FallbackTutorGateway(TutorGateway primaryAi, RuleBasedTutor offlineTutor) {
        this.primaryAi = primaryAi;
        this.offlineTutor = offlineTutor != null ? offlineTutor : new RuleBasedTutor();
    }

    /**
     * Requests a tutor reply from the primary gateway, falling back to the offline tutor on failure.
     *
     * @param request the tutor request containing conversation, circuit, and check state
     * @return a non-blank tutor reply or offline fallback hint
     */
    @Override
    public String reply(TutorRequest request) {
        try {
            if (primaryAi != null) {
                final String aiReply = primaryAi.reply(request);
                if (aiReply != null && !aiReply.isBlank()) {
                    return aiReply;
                }
            }
        } catch (TutorUnavailableException ignored) {
            // Fall back to rule-based offline tutor when primary AI is unavailable.
        }

        final String studentMsg = extractLatestMessage(request);
        final String lastCheck = request != null
                ? request.lastCheck().map(Object::toString).orElse(null)
                : null;
        return offlineTutor.generateOfflineResponse(studentMsg, lastCheck);
    }

    private String extractLatestMessage(TutorRequest request) {
        if (request == null) {
            return "";
        }
        final Conversation conversation = request.conversation();
        final List<ChatMessage> messages = conversation.messages();
        if (messages.isEmpty()) {
            return "";
        }
        final ChatMessage last = messages.get(messages.size() - 1);
        return last != null && last.text() != null ? last.text() : "";
    }
}
