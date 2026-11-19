package data_access.llm;

import entity.settings.AiSettings;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The client the rest of the app holds. Before each call it reads the current settings and, if
 * they changed (for example the user saved a new key), rebuilds its provider chain, so features
 * pick up new keys without restarting.
 */
public final class SettingsBackedLlmClient implements LlmClient {

    private final Supplier<AiSettings> settings;
    private final LlmClientFactory factory;
    private AiSettings currentSettings;
    private LlmClient currentClient;

    public SettingsBackedLlmClient(Supplier<AiSettings> settings, LlmClientFactory factory) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    @Override
    public String complete(LlmRequest request) throws LlmException {
        return client().complete(request);
    }

    private synchronized LlmClient client() {
        AiSettings latest = settings.get();
        if (currentClient == null || !latest.equals(currentSettings)) {
            currentSettings = latest;
            currentClient = factory.create(latest);
        }
        return currentClient;
    }
}
