package use_case;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TutorPromptBuilderTest {

    @Test
    void testBuildPromptHintLevels() {
        String baseMsg = "Why is my gate failing?";

        String prompt1 = TutorPromptBuilder.buildPrompt(baseMsg, 1);
        assertTrue(prompt1.contains("Hint 1"), "Prompt for hint 1 should mention Hint 1");

        String prompt2 = TutorPromptBuilder.buildPrompt(baseMsg, 2);
        assertTrue(prompt2.contains("Hint 2"), "Prompt for hint 2 should mention Hint 2");

        String prompt3 = TutorPromptBuilder.buildPrompt(baseMsg, 3);
        assertTrue(prompt3.contains("Hint 3"), "Prompt for hint 3 should mention Hint 3");

        String prompt0 = TutorPromptBuilder.buildPrompt(baseMsg, 0);
        assertEquals(baseMsg, prompt0, "Level 0 should return the original message unchanged");
    }
}