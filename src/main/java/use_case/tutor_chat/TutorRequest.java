package use_case.tutor_chat;

import entity.circuit.Circuit;
import entity.truthtable.CheckResult;
import entity.tutor.ChatRole;
import entity.tutor.Conversation;

import java.util.Objects;
import java.util.Optional;

/**
 * Everything a {@link TutorGateway} needs for one reply.
 *
 * @param conversation the chat so far; its last message is the student's new question
 * @param circuit      the circuit the student has open, if any
 * @param lastCheck    the latest check of that circuit, if any
 * @param hintLevel    0 for normal chat, 1 to 3 when the student pressed Hint 1 / 2 / 3
 */
public record TutorRequest(Conversation conversation, Optional<Circuit> circuit, Optional<CheckResult> lastCheck,
                           int hintLevel) {

    private static final int MAX_HINT_LEVEL = 3;

    /**
     * Creates a request for normal chat (hint level 0).
     *
     * @param conversation the chat so far
     * @param circuit      the open circuit, if any
     * @param lastCheck    the latest check, if any
     */
    public TutorRequest(Conversation conversation, Optional<Circuit> circuit, Optional<CheckResult> lastCheck) {
        this(conversation, circuit, lastCheck, 0);
    }

    public TutorRequest {
        Objects.requireNonNull(conversation, "conversation");
        Objects.requireNonNull(circuit, "circuit");
        Objects.requireNonNull(lastCheck, "lastCheck");
        if (hintLevel < 0 || hintLevel > MAX_HINT_LEVEL) {
            throw new IllegalArgumentException("hint level must be 0 to " + MAX_HINT_LEVEL);
        }
        if (conversation.isEmpty() || conversation.messages().get(conversation.messages().size() - 1).role()
                != ChatRole.STUDENT) {
            throw new IllegalArgumentException("the conversation must end with the student's message");
        }
    }
}
