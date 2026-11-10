package use_case.ai_settings;

/** Shows, saves and tests the user's AI settings. */
public interface AiSettingsInputBoundary {

    /** Presents the saved settings, for example when the Settings tab opens. */
    void load();

    void save(SaveAiSettingsInputData inputData);

    /** Sends a tiny request with the given key and presents whether it worked. May take seconds. */
    void testConnection(TestConnectionInputData inputData);
}
