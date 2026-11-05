package data_access.llm;

import java.util.Objects;

/** One message of a conversation sent to the AI. */
public record LlmMessage(LlmRole role, String text) {

    public LlmMessage {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            throw new IllegalArgumentException("message text must not be blank");
        }
    }

    public static LlmMessage user(String text) {
        return new LlmMessage(LlmRole.USER, text);
    }

    public static LlmMessage assistant(String text) {
        return new LlmMessage(LlmRole.ASSISTANT, text);
    }
}
