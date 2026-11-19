package interface_adapter.tutor_chat;

import java.util.List;

/** What the Tutor tab shows. */
public final class TutorChatState {

    private List<ChatBubble> messages = List.of();
    private String error = "";

    public TutorChatState() {
    }

    public TutorChatState(TutorChatState copy) {
        messages = copy.messages;
        error = copy.error;
    }

    /** @return the conversation, oldest first */
    public List<ChatBubble> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatBubble> messages) {
        this.messages = List.copyOf(messages);
    }

    /** @return the error to show, or "" if there is none */
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
