package use_case.ai_settings;

/**
 * @param providerId the provider to test, for example "gemini"
 * @param apiKey     the key as typed; it does not need to be saved first
 */
public record TestConnectionInputData(String providerId, String apiKey) {
}
