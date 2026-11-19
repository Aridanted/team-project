package entity.tutor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationTest {

    @Test
    void appendReturnsNewConversation() {
        Conversation empty = Conversation.empty();

        Conversation one = empty.append(ChatMessage.fromStudent("Hi"));

        assertTrue(empty.isEmpty());
        assertEquals(List.of(ChatMessage.fromStudent("Hi")), one.messages());
    }

    @Test
    void keepsOnlyTheLastMessages() {
        Conversation conversation = Conversation.empty();
        for (int i = 1; i <= Conversation.MAX_MESSAGES + 3; i++) {
            conversation = conversation.append(ChatMessage.fromStudent("m" + i));
        }

        assertEquals(Conversation.MAX_MESSAGES, conversation.messages().size());
        assertEquals("m4", conversation.messages().get(0).text());
        assertEquals("m" + (Conversation.MAX_MESSAGES + 3),
                conversation.messages().get(Conversation.MAX_MESSAGES - 1).text());
    }

    @Test
    void messagesCannotBeChanged() {
        Conversation conversation = Conversation.empty().append(ChatMessage.fromTutor("Hello"));

        assertThrows(UnsupportedOperationException.class,
                () -> conversation.messages().add(ChatMessage.fromStudent("x")));
    }

    @Test
    void rejectsBlankMessage() {
        assertThrows(IllegalArgumentException.class, () -> ChatMessage.fromStudent(" "));
    }
}
