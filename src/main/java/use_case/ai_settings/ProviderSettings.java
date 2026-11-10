package use_case.ai_settings;

/**
 * One provider's settings, as text.
 *
 * @param id          for example "gemini"
 * @param displayName for example "Gemini"
 * @param apiKey      the saved key, or "" if there is none
 */
public record ProviderSettings(String id, String displayName, String apiKey) {

    /** Never prints the key. */
    @Override
    public String toString() {
        return "ProviderSettings[id=" + id + ", hasKey=" + !apiKey.isEmpty() + "]";
    }
}
