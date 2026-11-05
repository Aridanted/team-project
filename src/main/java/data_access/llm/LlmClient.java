package data_access.llm;

/**
 * Sends one request to an AI model and returns its reply text. Implementations are the provider
 * clients ({@link GeminiClient}, {@link GroqClient}) and decorators that add fallback, rate
 * limiting or settings reloading. Tests use {@code FakeLlmClient}.
 */
public interface LlmClient {

    /**
     * @return the reply text, never blank
     * @throws LlmException if no usable reply was obtained; {@link LlmException#kind()} says why
     */
    String complete(LlmRequest request) throws LlmException;
}
