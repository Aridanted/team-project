package use_case.ai_settings;

import java.util.List;

/**
 * @param providers every provider, in the order they are tried
 * @param saved     whether these settings were just saved (rather than loaded)
 */
public record AiSettingsOutputData(List<ProviderSettings> providers, boolean saved) {

    public AiSettingsOutputData {
        providers = List.copyOf(providers);
    }
}
