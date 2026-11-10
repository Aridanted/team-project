package use_case.ai_settings;

/** Receives the outcome of loading, saving or testing AI settings. */
public interface AiSettingsOutputBoundary {

    void prepareSettingsView(AiSettingsOutputData outputData);

    void prepareConnectionView(ConnectionTestOutputData outputData);

    /** @param errorMessage what went wrong, ready to show to the user */
    void prepareFailView(String errorMessage);
}
