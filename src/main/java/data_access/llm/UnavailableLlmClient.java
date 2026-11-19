package data_access.llm;

/**
 * Null object used when no API key is configured: every call fails with
 * {@link LlmException.Kind#NO_KEY}, so features switch to their offline fallbacks.
 */
public final class UnavailableLlmClient implements LlmClient {

    static final String MESSAGE = "No AI key is set. Add one in the Settings tab.";

    @Override
    public String complete(LlmRequest request) throws LlmException {
        throw new LlmException(LlmException.Kind.NO_KEY, MESSAGE);
    }
}
