package use_case.tutor_chat;

import entity.circuit.Circuit;
import entity.circuit.CircuitTestFactory;
import entity.truthtable.CheckResult;
import entity.truthtable.TruthTable;
import entity.tutor.ChatMessage;
import entity.tutor.Conversation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TutorChatInteractorTest {

    private static final class FakeSession implements TutorChatDataAccessInterface {
        private Circuit circuit;
        private CheckResult lastCheck;
        private Conversation conversation = Conversation.empty();

        @Override
        public Optional<Circuit> getCurrentCircuit() {
            return Optional.ofNullable(circuit);
        }

        @Override
        public Optional<CheckResult> getLastCheck() {
            return Optional.ofNullable(lastCheck);
        }

        @Override
        public Conversation getConversation() {
            return conversation;
        }

        @Override
        public void saveConversation(Conversation conversation) {
            this.conversation = conversation;
        }
    }

    /** Replies with scripted text, or fails when {@code failure} is set. */
    private static final class FakeTutor implements TutorGateway {
        private final List<TutorRequest> requests = new ArrayList<>();
        private String reply = "What does an XOR gate output when both inputs are 1?";
        private String failure;

        @Override
        public String reply(TutorRequest request) throws TutorUnavailableException {
            requests.add(request);
            if (failure != null) {
                throw new TutorUnavailableException(failure);
            }
            return reply;
        }
    }

    private static final class CapturingPresenter implements TutorChatOutputBoundary {
        private TutorChatOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(TutorChatOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }

    private final FakeSession session = new FakeSession();
    private final FakeTutor tutor = new FakeTutor();
    private final CapturingPresenter presenter = new CapturingPresenter();
    private final TutorChatInteractor interactor = new TutorChatInteractor(session, tutor, presenter);

    @Test
    void sendsQuestionAndSavesBothMessages() {
        interactor.send(new TutorChatInputData("  Why is Sum wrong?  "));

        assertNull(presenter.error);
        assertEquals(List.of(new TutorChatOutputData.ChatLine(true, "Why is Sum wrong?"),
                new TutorChatOutputData.ChatLine(false, tutor.reply)), presenter.success.messages());
        assertEquals(List.of(ChatMessage.fromStudent("Why is Sum wrong?"), ChatMessage.fromTutor(tutor.reply)),
                session.conversation.messages());
    }

    @Test
    void givesTheTutorTheCircuitTheCheckAndTheWholeChat() {
        session.circuit = CircuitTestFactory.halfAdder();
        session.lastCheck = new CheckResult(new TruthTable(List.of("A"), List.of("Y"), List.of()), List.of());
        session.conversation = Conversation.empty().append(ChatMessage.fromStudent("Hi"))
                .append(ChatMessage.fromTutor("Hello!"));

        interactor.send(new TutorChatInputData("Help"));

        TutorRequest request = tutor.requests.get(0);
        assertSame(session.circuit, request.circuit().orElseThrow());
        assertSame(session.lastCheck, request.lastCheck().orElseThrow());
        assertEquals(List.of(ChatMessage.fromStudent("Hi"), ChatMessage.fromTutor("Hello!"),
                ChatMessage.fromStudent("Help")), request.conversation().messages());
        assertEquals(4, session.conversation.messages().size());
    }

    @Test
    void worksWithoutACircuit() {
        interactor.send(new TutorChatInputData("What is a NAND gate?"));

        assertTrue(tutor.requests.get(0).circuit().isEmpty());
        assertTrue(tutor.requests.get(0).lastCheck().isEmpty());
        assertNull(presenter.error);
    }

    @Test
    void rejectsEmptyMessage() {
        interactor.send(new TutorChatInputData("   "));

        assertEquals("Message cannot be empty!", presenter.error);
        assertTrue(tutor.requests.isEmpty());
    }

    @Test
    void rejectsNullMessage() {
        interactor.send(new TutorChatInputData(null));

        assertEquals("Message cannot be empty!", presenter.error);
    }

    @Test
    void rejectsTooLongMessage() {
        interactor.send(new TutorChatInputData("a".repeat(TutorChatInteractor.MAX_MESSAGE_LENGTH + 1)));

        assertTrue(presenter.error.startsWith("Message is too long"));
        assertTrue(tutor.requests.isEmpty());
    }

    @Test
    void acceptsMessageOfMaximumLength() {
        interactor.send(new TutorChatInputData("a".repeat(TutorChatInteractor.MAX_MESSAGE_LENGTH)));

        assertNull(presenter.error);
    }

    @Test
    void keepsConversationWhenTutorIsUnavailable() {
        tutor.failure = "No AI key is set. Add one in the Settings tab.";

        interactor.send(new TutorChatInputData("Hello?"));

        assertEquals("The tutor could not answer: No AI key is set. Add one in the Settings tab.", presenter.error);
        assertTrue(session.conversation.isEmpty());
        assertNull(presenter.success);
    }

    @Test
    void treatsBlankReplyAsFailure() {
        tutor.reply = "  ";

        interactor.send(new TutorChatInputData("Hello?"));

        assertEquals("The tutor could not answer: the reply was empty.", presenter.error);
        assertTrue(session.conversation.isEmpty());
    }

    @Test
    void clearForgetsConversation() {
        interactor.send(new TutorChatInputData("Hello?"));

        interactor.clear();

        assertTrue(session.conversation.isEmpty());
        assertEquals(List.of(), presenter.success.messages());
    }
}
