package data_access.llm;

import entity.settings.AiProvider;

import java.util.List;

/** The providers LogiCoach supports, with their environment variables and default models. */
public final class LlmProviders {

    /**
     * Cheapest Gemini model that handles the tutor and builder well. Override with GEMINI_MODEL.
     * Google no longer offers gemini-2.5-flash-lite to new keys (HTTP 404, issue #15) and its
     * error message recommends this model instead.
     */
    public static final String DEFAULT_GEMINI_MODEL = "gemini-3.5-flash-lite";

    /** Cheapest Claude model, fast enough for chat. Override with ANTHROPIC_MODEL. */
    public static final String DEFAULT_CLAUDE_MODEL = "claude-haiku-5-5";

    /** Cheapest ChatGPT model that follows the tutor rules well. Override with OPENAI_MODEL. */
    public static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";

    private LlmProviders() {
    }

    public static List<LlmProviderSpec> standard(HttpTransport transport) {
        return List.of(
                new LlmProviderSpec(AiProvider.CLAUDE, "ANTHROPIC_API_KEY", "ANTHROPIC_MODEL", DEFAULT_CLAUDE_MODEL,
                        (key, model) -> new ClaudeClient(transport, key, model)),
                new LlmProviderSpec(AiProvider.GEMINI, "GEMINI_API_KEY", "GEMINI_MODEL", DEFAULT_GEMINI_MODEL,
                        (key, model) -> new GeminiClient(transport, key, model)),
                new LlmProviderSpec(AiProvider.CHATGPT, "OPENAI_API_KEY", "OPENAI_MODEL", DEFAULT_OPENAI_MODEL,
                        (key, model) -> new OpenAiClient(transport, key, model)));
    }
}
