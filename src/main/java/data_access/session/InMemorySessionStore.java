package data_access.session;

import entity.circuit.Circuit;
import entity.truthtable.CheckResult;
import entity.tutor.Conversation;
import use_case.check_circuit.CheckCircuitDataAccessInterface;
import use_case.import_circuit.ImportCircuitDataAccessInterface;
import use_case.tutor_chat.TutorChatDataAccessInterface;

import java.util.Objects;
import java.util.Optional;

/**
 * In-memory holder for the circuit the user has open, its latest check result and the tutor chat.
 * Implements each use case's small session interface (interface segregation), so use cases share
 * data without calling each other.
 */
public final class InMemorySessionStore implements ImportCircuitDataAccessInterface,
        CheckCircuitDataAccessInterface, TutorChatDataAccessInterface {

    private Circuit currentCircuit;
    private CheckResult lastCheck;
    private Conversation conversation = Conversation.empty();

    /** Also forgets the previous check, which belonged to the previous circuit. */
    @Override
    public synchronized void setCurrentCircuit(Circuit circuit) {
        this.currentCircuit = Objects.requireNonNull(circuit, "circuit");
        this.lastCheck = null;
    }

    /** @return the circuit last opened successfully, if any */
    @Override
    public synchronized Optional<Circuit> getCurrentCircuit() {
        return Optional.ofNullable(currentCircuit);
    }

    @Override
    public synchronized void setLastCheck(CheckResult result) {
        this.lastCheck = Objects.requireNonNull(result, "result");
    }

    /** @return the latest check of the current circuit, if any */
    @Override
    public synchronized Optional<CheckResult> getLastCheck() {
        return Optional.ofNullable(lastCheck);
    }

    /** The chat survives opening another circuit, so the student can keep talking about a fix. */
    @Override
    public synchronized Conversation getConversation() {
        return conversation;
    }

    @Override
    public synchronized void saveConversation(Conversation conversation) {
        this.conversation = Objects.requireNonNull(conversation, "conversation");
    }
}
