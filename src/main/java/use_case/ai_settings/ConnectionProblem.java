package use_case.ai_settings;

/** Why a connection test failed, in terms the user can act on. */
public enum ConnectionProblem {
    /** No key was given. */
    NO_KEY,
    /** The provider could not be reached. */
    NETWORK,
    /** Too many requests, or the free quota is used up. */
    RATE_LIMIT,
    /** The key was rejected. */
    AUTH,
    /** The provider answered with something unusable. */
    BAD_RESPONSE,
    /** The provider refused to answer the test request. */
    BLOCKED
}
