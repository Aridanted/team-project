package use_case.tutor_chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import use_case.FallbackTutorGateway;
import use_case.RuleBasedTutor;

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
}
