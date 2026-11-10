package data_access.llm;

/** Who wrote a message in a conversation sent to the AI. */
public enum LlmRole {
    /** The app, speaking for the student. */
    USER,
    /** The AI's earlier replies. */
    ASSISTANT
}
