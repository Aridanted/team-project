package interface_adapter.tutor_chat;

/**
 * One message as the Tutor tab shows it.
 *
 * @param fromStudent whether the student wrote it (shown on the right) or the tutor (left)
 * @param sender      "You" or "Tutor"
 * @param offline     whether the offline tutor wrote it, so the view marks the bubble with an Offline badge
 */
public record ChatBubble(boolean fromStudent, String sender, String text, boolean offline) {

    /**
     * Creates a bubble that is not marked offline.
     *
     * @param fromStudent whether the student wrote it
     * @param sender      "You" or "Tutor"
     * @param text        what it says
     */
    public ChatBubble(boolean fromStudent, String sender, String text) {
        this(fromStudent, sender, text, false);
    }
}
