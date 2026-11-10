package use_case.ai_settings;

import java.util.List;
import java.util.Map;

/**
 * @param providerOrder provider ids in the order to try them, for example ["groq", "gemini"]
 * @param apiKeys       API key per provider id; a blank key removes the saved key
 */
public record SaveAiSettingsInputData(List<String> providerOrder, Map<String, String> apiKeys) {

    public SaveAiSettingsInputData {
        providerOrder = List.copyOf(providerOrder);
        apiKeys = Map.copyOf(apiKeys);
    }
}
