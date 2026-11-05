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
 */
public record TutorRequest(Conversation conversation, Optional<Circuit> circuit, Optional<CheckResult> lastCheck) {

    public TutorRequest {
        Objects.requireNonNull(conversation, "conversation");
        Objects.requireNonNull(circuit, "circuit");
        Objects.requireNonNull(lastCheck, "lastCheck");
        if (conversation.isEmpty() || conversation.messages().get(conversation.messages().size() - 1).role()
                != ChatRole.STUDENT) {
            throw new IllegalArgumentException("the conversation must end with the student's message");
        }
    }
}
