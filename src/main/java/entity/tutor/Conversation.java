package entity.tutor;

import java.util.ArrayList;
import java.util.List;

/**
 * The tutor chat so far, oldest first. Immutable: {@link #append} returns a new conversation.
 * Only the last {@value #MAX_MESSAGES} messages are kept, so prompts stay small.
 */
public final class Conversation {

    public static final int MAX_MESSAGES = 20;

    private static final Conversation EMPTY = new Conversation(List.of());

    private final List<ChatMessage> messages;

    private Conversation(List<ChatMessage> messages) {
        this.messages = List.copyOf(messages);
    }

    public static Conversation empty() {
        return EMPTY;
    }

    /** @return a conversation with {@code message} added at the end, dropping the oldest if full */
    public Conversation append(ChatMessage message) {
        List<ChatMessage> next = new ArrayList<>(messages);
        next.add(message);
        if (next.size() > MAX_MESSAGES) {
            next = next.subList(next.size() - MAX_MESSAGES, next.size());
        }
        return new Conversation(next);
    }

    public List<ChatMessage> messages() {
        return messages;
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Conversation that && messages.equals(that.messages);
    }

    @Override
    public int hashCode() {
        return messages.hashCode();
    }

    @Override
    public String toString() {
        return "Conversation" + messages;
    }
}
