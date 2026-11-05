package use_case.tutor_chat;

import java.util.List;

/** @param messages the whole conversation, oldest first */
public record TutorChatOutputData(List<ChatLine> messages) {

    /** One message, without exposing the entity to outer layers. */
    public record ChatLine(boolean fromStudent, String text) {
    }

    public TutorChatOutputData {
        messages = List.copyOf(messages);
    }
}
