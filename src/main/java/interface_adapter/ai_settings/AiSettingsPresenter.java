package interface_adapter.ai_settings;

import use_case.ai_settings.AiSettingsOutputBoundary;
import use_case.ai_settings.AiSettingsOutputData;
import use_case.ai_settings.ConnectionProblem;
import use_case.ai_settings.ConnectionTestOutputData;

import java.util.Objects;

/** Turns settings and connection results into plain words for {@link AiSettingsState}. */
public final class AiSettingsPresenter implements AiSettingsOutputBoundary {

    static final String SAVED = "Settings saved.";

    private final AiSettingsViewModel viewModel;

    public AiSettingsPresenter(AiSettingsViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
    }

    @Override
    public void prepareSettingsView(AiSettingsOutputData outputData) {
        AiSettingsState state = new AiSettingsState(viewModel.getState());
        state.setProviders(outputData.providers().stream()
                .map(p -> new ProviderRow(p.id(), p.displayName(), p.apiKey()))
                .toList());
        state.setStatus(outputData.saved() ? SAVED : "");
        state.setError("");
        publish(state);
    }

    @Override
    public void prepareConnectionView(ConnectionTestOutputData outputData) {
        AiSettingsState state = new AiSettingsState(viewModel.getState());
        state.setConnected(outputData.isConnected());
        state.setConnectionMessage(outputData.isConnected()
                ? "Connected to " + outputData.providerName() + "."
                : explain(outputData.providerName(), outputData.problem()));
        state.setError("");
        publish(state);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        AiSettingsState state = new AiSettingsState(viewModel.getState());
        state.setError(errorMessage);
        state.setStatus("");
        publish(state);
    }

    static String explain(String provider, ConnectionProblem problem) {
        return switch (problem) {
            case NO_KEY -> "There is no " + provider + " API key to test.";
            case AUTH -> provider + " rejected the API key. Check that you copied all of it and that it is "
                    + "still active.";
            case RATE_LIMIT -> provider + " says there were too many requests. Wait a minute and try again.";
            case NETWORK -> "Could not reach " + provider + ". Check your internet connection and try again.";
            case BAD_RESPONSE -> provider + " answered in an unexpected way. Try again later, or try another "
                    + "provider.";
            case BLOCKED -> provider + " refused to answer the test message. Try again later.";
        };
    }

    private void publish(AiSettingsState state) {
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }
}
