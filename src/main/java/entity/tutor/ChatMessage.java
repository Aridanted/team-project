package entity.tutor;

import java.util.Objects;

/** One message of the tutor chat. */
public record ChatMessage(ChatRole role, String text) {

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
}
