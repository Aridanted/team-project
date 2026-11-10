package interface_adapter.tutor_chat;

/**
 * One message as the Tutor tab shows it.
 *
 * @param fromStudent whether the student wrote it (shown on the right) or the tutor (left)
 * @param sender      "You" or "Tutor"
 */
public record ChatBubble(boolean fromStudent, String sender, String text) {
}
