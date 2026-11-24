package use_case.tutor_chat;

/**
 * Builds prompts tailored to the requested hint level (0 for normal chat, 1 to 3 for hints).
 */
public final class HintPromptBuilder {

    private HintPromptBuilder() {
    }

    /**
     * Builds the prompt for the given student message and hint level.
     *
     * @param studentMessage the student's message
     * @param hintLevel 0 for normal chat, 1 for gentle nudge, 2 for faulty part, 3 for concept
     * @return the prompt string to send
     */
    public static String buildPrompt(String studentMessage, int hintLevel) {
        final String safeMessage = studentMessage == null ? "" : studentMessage;
        switch (hintLevel) {
            case 1:
                return "Give a gentle nudge (Hint 1) without revealing the answer: " + safeMessage;
            case 2:
                return "Point out the likely faulty part of the circuit (Hint 2) for: " + safeMessage;
            case 3:
                return "Explain the concept behind this circuit (Hint 3) for: " + safeMessage;
            default:
                return safeMessage;
        }
    }
}
