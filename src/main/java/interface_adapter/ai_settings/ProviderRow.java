package interface_adapter.ai_settings;

/**
 * One provider on the settings screen.
 *
 * @param id          for example "gemini"; pass it back to the controller
 * @param displayName for example "Gemini"
 * @param apiKey      the saved key, or ""; views show it masked
 */
public record ProviderRow(String id, String displayName, String apiKey) {

    /** Never prints the key. */
    @Override
    public String toString() {
        return "ProviderRow[id=" + id + ", hasKey=" + !apiKey.isEmpty() + "]";
    }
}
