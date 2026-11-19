package use_case;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FallbackTutorGatewayTest {

    @Test
    void testFallbackToOfflineWhenPrimaryFails() {
        TutorGateway failingAI = (msg, check) -> null;
        RuleBasedTutor offlineTutor = new RuleBasedTutor();

        FallbackTutorGateway gateway = new FallbackTutorGateway(failingAI, offlineTutor);

        TutorResponse response = gateway.getAITutorResponse("Help me", "FAIL at row 2");

        assertNotNull(response);
        assertTrue(response.isOffline());
        assertTrue(response.getMessage().contains("Offline Tutor"));
    }
}