package interface_adapter.ai_settings;

import use_case.ai_settings.AiSettingsInputBoundary;
import use_case.ai_settings.SaveAiSettingsInputData;
import use_case.ai_settings.TestConnectionInputData;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Called by the Settings tab. */
public final class AiSettingsController {

    private final AiSettingsInputBoundary interactor;

    public AiSettingsController(AiSettingsInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor, "interactor");
    }

    /** Shows the saved settings. */
    public void load() {
        interactor.load();
    }

    /**
     * @param providerOrder provider ids, first one tried first
     * @param apiKeys       key per provider id as typed; blank removes the key
     */
    public void save(List<String> providerOrder, Map<String, String> apiKeys) {
        interactor.save(new SaveAiSettingsInputData(providerOrder, apiKeys));
    }

    /** Sends a tiny request with this key. Slow: call it off the Swing event thread. */
    public void testConnection(String providerId, String apiKey) {
        interactor.testConnection(new TestConnectionInputData(providerId, apiKey));
    }
}
