package use_case.ai_settings;

import entity.settings.AiProvider;
import entity.settings.AiSettings;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Loads and saves AI settings, and tests a provider's key with a tiny request. */
public final class AiSettingsInteractor implements AiSettingsInputBoundary {

    private final AiSettingsDataAccessInterface settingsStore;
    private final AiConnectionTester connectionTester;
    private final AiSettingsOutputBoundary presenter;

    public AiSettingsInteractor(AiSettingsDataAccessInterface settingsStore, AiConnectionTester connectionTester,
                                AiSettingsOutputBoundary presenter) {
        this.settingsStore = Objects.requireNonNull(settingsStore, "settingsStore");
        this.connectionTester = Objects.requireNonNull(connectionTester, "connectionTester");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    /** If the saved settings cannot be read, shows defaults and the error, so the user can save new ones. */
    @Override
    public void load() {
        try {
            presenter.prepareSettingsView(toOutputData(settingsStore.load(), false));
        } catch (SettingsStorageException e) {
            presenter.prepareSettingsView(toOutputData(AiSettings.defaults(), false));
            presenter.prepareFailView(e.getMessage());
        }
    }

    @Override
    public void save(SaveAiSettingsInputData inputData) {
        Objects.requireNonNull(inputData, "inputData");
        final AiSettings settings;
        try {
            settings = new AiSettings(parseOrder(inputData.providerOrder()), parseKeys(inputData.apiKeys()));
        } catch (IllegalArgumentException e) {
            presenter.prepareFailView(e.getMessage());
            return;
        }
        try {
            settingsStore.save(settings);
        } catch (SettingsStorageException e) {
            presenter.prepareFailView(e.getMessage());
            return;
        }
        presenter.prepareSettingsView(toOutputData(settings, true));
    }

    @Override
    public void testConnection(TestConnectionInputData inputData) {
        Objects.requireNonNull(inputData, "inputData");
        Optional<AiProvider> found = AiProvider.fromId(inputData.providerId());
        if (found.isEmpty()) {
            presenter.prepareFailView(unknownProvider(inputData.providerId()));
            return;
        }
        AiProvider provider = found.get();
        final String key;
        try {
            key = new AiSettings(List.of(), Map.of(provider, orEmpty(inputData.apiKey()))).apiKey(provider).orElse("");
        } catch (IllegalArgumentException e) {
            presenter.prepareFailView(e.getMessage());
            return;
        }
        if (key.isEmpty()) {
            presenter.prepareFailView("Enter a " + provider.displayName() + " API key first.");
            return;
        }
        try {
            connectionTester.test(provider, key);
            presenter.prepareConnectionView(ConnectionTestOutputData.connected(provider.displayName()));
        } catch (AiConnectionException e) {
            presenter.prepareConnectionView(new ConnectionTestOutputData(provider.displayName(), e.problem()));
        }
    }

    private static List<AiProvider> parseOrder(List<String> ids) {
        List<AiProvider> order = new ArrayList<>();
        for (String id : ids) {
            order.add(AiProvider.fromId(id).orElseThrow(() -> new IllegalArgumentException(unknownProvider(id))));
        }
        return order;
    }

    private static Map<AiProvider, String> parseKeys(Map<String, String> keysById) {
        Map<AiProvider, String> keys = new EnumMap<>(AiProvider.class);
        for (Map.Entry<String, String> entry : keysById.entrySet()) {
            AiProvider provider = AiProvider.fromId(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException(unknownProvider(entry.getKey())));
            keys.put(provider, orEmpty(entry.getValue()));
        }
        return keys;
    }

    private static AiSettingsOutputData toOutputData(AiSettings settings, boolean saved) {
        List<ProviderSettings> providers = settings.providerOrder().stream()
                .map(provider -> new ProviderSettings(provider.id(), provider.displayName(),
                        settings.apiKey(provider).orElse("")))
                .toList();
        return new AiSettingsOutputData(providers, saved);
    }

    private static String unknownProvider(String id) {
        return "Unknown AI provider \"" + id + "\".";
    }

    private static String orEmpty(String text) {
        return text == null ? "" : text;
    }
}
