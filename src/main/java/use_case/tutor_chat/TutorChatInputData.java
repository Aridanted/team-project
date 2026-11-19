package use_case.tutor_chat;

/**
 * Input data representing a student message and optional hint level (0 = normal chat, 1-3 = hints).
 *
 * @param message what the student typed
 * @param hintLevel requested hint level (0 for normal chat, 1 to 3 for hints)
 */
public record TutorChatInputData(String message, int hintLevel) {

    private static final int MAX_HINT_LEVEL = 3;

    /**
     * Constructs TutorChatInputData for standard chat (hint level 0).
     *
     * @param message what the student typed
     */
    public TutorChatInputData(String message) {
        this(message, 0);
    }

    /**
     * Compact constructor normalizing invalid hint levels to 0.
     */
    public TutorChatInputData {
        if (hintLevel < 0 || hintLevel > MAX_HINT_LEVEL) {
            hintLevel = 0;
        }
    }
}
