package entity.tutor;

import java.util.Objects;

/**
 * One message of the tutor chat.
 *
 * @param role    who wrote it
 * @param text    what it says
 * @param offline whether the offline rule-based tutor wrote it, because the AI tutor was unavailable
 */
public record ChatMessage(ChatRole role, String text, boolean offline) {

    /**
     * Creates a message that was not written by the offline tutor.
     *
     * @param role who wrote it
     * @param text what it says
     */
    public ChatMessage(ChatRole role, String text) {
        this(role, text, false);
    }

    public ChatMessage {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            throw new IllegalArgumentException("a chat message must not be blank");
        }
    }

    public static ChatMessage fromStudent(String text) {
        return new ChatMessage(ChatRole.STUDENT, text);
    }

    public static ChatMessage fromTutor(String text) {
        return new ChatMessage(ChatRole.TUTOR, text);
    }

    public static ChatMessage fromOfflineTutor(String text) {
        return new ChatMessage(ChatRole.TUTOR, text, true);
    }
}
