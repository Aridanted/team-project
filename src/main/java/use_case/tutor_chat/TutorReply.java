package use_case.tutor_chat;

import java.util.Objects;

/**
 * A tutor's answer and where it came from.
 *
 * @param text    the reply text
 * @param offline whether the built-in rule-based tutor wrote it because the AI tutor was unavailable
 */
public record TutorReply(String text, boolean offline) {

    public TutorReply {
        Objects.requireNonNull(text, "text");
    }

    /**
     * Creates a reply written by the AI tutor.
     *
     * @param text the reply text
     * @return the reply, marked as not offline
     */
    public static TutorReply online(String text) {
        return new TutorReply(text, false);
    }

    /**
     * Creates a reply written by the offline rule-based tutor.
     *
     * @param text the reply text
     * @return the reply, marked as offline
     */
    public static TutorReply offline(String text) {
        return new TutorReply(text, true);
    }
}
