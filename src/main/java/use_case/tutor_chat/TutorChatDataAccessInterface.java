package use_case.tutor_chat;

import entity.circuit.Circuit;
import entity.truthtable.CheckResult;
import entity.tutor.Conversation;

import java.util.Optional;

/** What the tutor needs from the session: the open circuit, its last check, and the chat so far. */
public interface TutorChatDataAccessInterface {

    Optional<Circuit> getCurrentCircuit();

    Optional<CheckResult> getLastCheck();

    Conversation getConversation();

    void saveConversation(Conversation conversation);
}
