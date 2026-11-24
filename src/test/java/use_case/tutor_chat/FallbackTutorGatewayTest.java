package use_case.tutor_chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import entity.tutor.ChatMessage;
import entity.tutor.Conversation;


class FallbackTutorGatewayTest {

    @Test
    void returnsPrimaryReplyWhenAvailable() {
        final TutorGateway primaryAi = request -> "Primary AI hint";
        final FallbackTutorGateway gateway = new FallbackTutorGateway(primaryAi, new RuleBasedTutor());

        final String reply = gateway.reply(null);

        assertEquals("Primary AI hint", reply);
    }

    @Test
    void fallsBackToRuleBasedTutorWhenPrimaryThrowsUnavailable() {
        final TutorGateway failingPrimary = request -> {
            throw new TutorUnavailableException("API down");
        };
        final FallbackTutorGateway gateway = new FallbackTutorGateway(failingPrimary, new RuleBasedTutor());

        final String reply = gateway.reply(null);

        assertNotNull(reply);
        assertFalse(reply.isBlank());
    }

    @Test
    void fallsBackToRuleBasedTutorWhenPrimaryReturnsBlank() {
        final TutorGateway blankPrimary = request -> "   ";
        final FallbackTutorGateway gateway = new FallbackTutorGateway(blankPrimary, new RuleBasedTutor());

        final String reply = gateway.reply(null);

        assertNotNull(reply);
        assertFalse(reply.isBlank());
    }

    @Test
    void onlineAnswerIsNotMarkedOffline() {
        final FallbackTutorGateway gateway = new FallbackTutorGateway(request -> "AI hint", new RuleBasedTutor());

        final TutorReply answer = gateway.answer(request(0));

        assertEquals("AI hint", answer.text());
        assertFalse(answer.offline());
    }

    @Test
    void fallbackAnswerIsMarkedOfflineAndFollowsTheHintLevel() {
        final TutorGateway failing = request -> {
            throw new TutorUnavailableException("API down");
        };
        final FallbackTutorGateway gateway = new FallbackTutorGateway(failing, new RuleBasedTutor());

        final TutorReply hint1 = gateway.answer(request(1));
        final TutorReply hint3 = gateway.answer(request(3));

        assertTrue(hint1.offline());
        assertTrue(hint1.text().contains("Hint 1"));
        assertTrue(hint3.text().contains("Hint 3"));
    }

    private static TutorRequest request(int hintLevel) {
        return new TutorRequest(Conversation.empty().append(ChatMessage.fromStudent("Help")),
                Optional.empty(), Optional.empty(), hintLevel);
    }
}
