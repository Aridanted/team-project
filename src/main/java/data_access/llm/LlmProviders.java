package data_access.llm;

import entity.settings.AiProvider;

import java.util.List;

/** The providers LogiCoach supports, with their environment variables and default models. */
public final class LlmProviders {

    /** Cheapest Gemini model that handles the tutor and builder well. Override with GEMINI_MODEL. */
    public static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash-lite";

    /** Small, fast Llama model on Groq's free tier. Override with GROQ_MODEL. */
    public static final String DEFAULT_GROQ_MODEL = "llama-3.1-8b-instant";

    private LlmProviders() {
    }

    public static List<LlmProviderSpec> standard(HttpTransport transport) {
        return List.of(
                new LlmProviderSpec(AiProvider.GEMINI, "GEMINI_API_KEY", "GEMINI_MODEL", DEFAULT_GEMINI_MODEL,
                        (key, model) -> new GeminiClient(transport, key, model)),
                new LlmProviderSpec(AiProvider.GROQ, "GROQ_API_KEY", "GROQ_MODEL", DEFAULT_GROQ_MODEL,
                        (key, model) -> new GroqClient(transport, key, model)));
    }
}
