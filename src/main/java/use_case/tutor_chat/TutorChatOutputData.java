package use_case.tutor_chat;

import java.util.List;

/** @param messages the whole conversation, oldest first */
public record TutorChatOutputData(List<ChatLine> messages) {

    /**
     * One message, without exposing the entity to outer layers.
     *
     * @param fromStudent whether the student wrote it
     * @param text        what it says
     * @param offline     whether the offline rule-based tutor wrote it
     */
    public record ChatLine(boolean fromStudent, String text, boolean offline) {

        /**
         * Creates a line that was not written by the offline tutor.
         *
         * @param fromStudent whether the student wrote it
         * @param text        what it says
         */
        public ChatLine(boolean fromStudent, String text) {
            this(fromStudent, text, false);
        }
    }

    public TutorChatOutputData {
        messages = List.copyOf(messages);
    }
}
