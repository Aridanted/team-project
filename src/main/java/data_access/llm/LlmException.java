package data_access.llm;

import java.util.Objects;

/**
 * Why an AI call failed. Messages never contain API keys or prompts, so they are safe to show
 * and to log.
 */
public class LlmException extends Exception {

    private static final long serialVersionUID = 1L;

    /** The kinds of failure callers react to differently. */
    public enum Kind {
        /** No API key is configured; use the offline fallback. */
        NO_KEY,
        /** The provider could not be reached, timed out, or had a server error. */
        NETWORK,
        /** Too many requests or quota used up; try again later. */
        RATE_LIMIT,
        /** The API key was rejected. */
        AUTH,
        /** The provider answered, but not with a usable reply. */
        BAD_RESPONSE,
        /** The provider refused to answer, for example a safety filter. */
        BLOCKED
    }

    private final Kind kind;

    public LlmException(Kind kind, String message) {
        this(kind, message, null);
    }

    public LlmException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public Kind kind() {
        return kind;
    }
}
