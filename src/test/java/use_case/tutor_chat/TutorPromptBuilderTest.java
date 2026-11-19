package use_case.tutor_chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import use_case.TutorPromptBuilder;

class TutorPromptBuilderTest {

    @Test
    void testBuildPromptHintLevels() {
        final String baseMsg = "Why is my gate failing?";

        final String prompt1 = TutorPromptBuilder.buildPrompt(baseMsg, 1);
        assertTrue(prompt1.contains("Hint 1"), "Prompt for hint 1 should mention Hint 1");

        final String prompt2 = TutorPromptBuilder.buildPrompt(baseMsg, 2);
        assertTrue(prompt2.contains("Hint 2"), "Prompt for hint 2 should mention Hint 2");

        final String prompt3 = TutorPromptBuilder.buildPrompt(baseMsg, 3);
        assertTrue(prompt3.contains("Hint 3"), "Prompt for hint 3 should mention Hint 3");

        final String prompt0 = TutorPromptBuilder.buildPrompt(baseMsg, 0);
        assertEquals(baseMsg, prompt0, "Level 0 should return the original message unchanged");
    }

    @Test
    void testBuildPromptNullMessage() {
        assertEquals("", TutorPromptBuilder.buildPrompt(null, 0));
        assertTrue(TutorPromptBuilder.buildPrompt(null, 1).contains("Hint 1"));
    }
}
