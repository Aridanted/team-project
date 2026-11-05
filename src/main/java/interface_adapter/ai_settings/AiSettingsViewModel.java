package interface_adapter.ai_settings;

import interface_adapter.ViewModel;

/** View model for the Settings tab. */
public final class AiSettingsViewModel extends ViewModel<AiSettingsState> {

    public AiSettingsViewModel() {
        super("ai settings", new AiSettingsState());
    }
}
