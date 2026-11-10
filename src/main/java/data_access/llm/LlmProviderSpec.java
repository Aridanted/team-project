package data_access.llm;

import entity.settings.AiProvider;

import java.util.Objects;

/**
 * How to build a client for one provider. Adding a provider means a new client class, a new
 * {@link AiProvider} value and one more spec in {@link LlmProviders}; the factory does not change.
 *
 * @param keyVariable   environment variable that may hold the API key, for example GEMINI_API_KEY
 * @param modelVariable environment variable that may override the model, for example GEMINI_MODEL
 * @param defaultModel  model used when the variable is not set (the cheapest suitable one)
 * @param maker         creates the client from a key and a model name
 */
public record LlmProviderSpec(AiProvider provider, String keyVariable, String modelVariable, String defaultModel,
                              ClientMaker maker) {

    /** Creates a provider client. */
    @FunctionalInterface
    public interface ClientMaker {
        LlmClient create(String apiKey, String model);
    }

    public LlmProviderSpec {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(keyVariable, "keyVariable");
        Objects.requireNonNull(modelVariable, "modelVariable");
        Objects.requireNonNull(defaultModel, "defaultModel");
        Objects.requireNonNull(maker, "maker");
    }
}
