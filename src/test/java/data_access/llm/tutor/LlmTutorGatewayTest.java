package data_access.llm.tutor;

import data_access.json.CircuitJsonCodec;
import data_access.llm.FakeLlmClient;
import data_access.llm.LlmException;
import data_access.llm.LlmMessage;
import data_access.llm.LlmRequest;
import entity.design.CircuitDescriber;
import entity.tutor.ChatMessage;
import entity.tutor.Conversation;
import org.junit.jupiter.api.Test;
import use_case.tutor_chat.TutorRequest;
import use_case.tutor_chat.TutorUnavailableException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmTutorGatewayTest {

    private final TutorPromptBuilder prompts = new TutorPromptBuilder(new CircuitDescriber(), new CircuitJsonCodec());

    private static TutorRequest ask(Conversation conversation) {
        return new TutorRequest(conversation, Optional.empty(), Optional.empty());
    }

    @Test
    void sendsWholeChatWithSystemPromptAndReturnsReply() throws TutorUnavailableException {
        FakeLlmClient ai = FakeLlmClient.replying("Which gate drives Sum?");
        LlmTutorGateway gateway = new LlmTutorGateway(ai, prompts);
        Conversation chat = Conversation.empty().append(ChatMessage.fromStudent("Hi"))
                .append(ChatMessage.fromTutor("Hello!")).append(ChatMessage.fromStudent("Sum is wrong"));

        String reply = gateway.reply(ask(chat));

        assertEquals("Which gate drives Sum?", reply);
        LlmRequest request = ai.lastRequest();
        assertEquals(prompts.systemPrompt(Optional.empty(), Optional.empty()), request.systemPrompt());
        assertEquals(List.of(LlmMessage.user("Hi"), LlmMessage.assistant("Hello!"), LlmMessage.user("Sum is wrong")),
                request.messages());
        assertEquals(LlmTutorGateway.TEMPERATURE, request.temperature());
        assertFalse(request.expectJson());
    }

    @Test
    void dropsTutorMessagesBeforeTheFirstQuestion() {
        List<LlmMessage> messages = LlmTutorGateway.toLlmMessages(List.of(
                ChatMessage.fromTutor("Welcome!"), ChatMessage.fromStudent("Hi")));

        assertEquals(List.of(LlmMessage.user("Hi")), messages);
    }

    @Test
    void turnsAiFailureIntoTutorUnavailable() {
        FakeLlmClient ai = FakeLlmClient.alwaysFailing(LlmException.Kind.NO_KEY);
        LlmTutorGateway gateway = new LlmTutorGateway(ai, prompts);

        TutorUnavailableException error = assertThrows(TutorUnavailableException.class,
                () -> gateway.reply(ask(Conversation.empty().append(ChatMessage.fromStudent("Hi")))));

        assertEquals("fake NO_KEY", error.getMessage());
    }
}
