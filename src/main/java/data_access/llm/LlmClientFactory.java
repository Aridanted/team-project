package data_access.llm;

import entity.settings.AiProvider;
import entity.settings.AiSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the {@link LlmClient} for the user's settings: one client per provider that has a key
 * (a saved key wins over the environment variable), tried in the saved order through
 * {@link FallbackLlmClient}. With no key at all it returns an {@link UnavailableLlmClient}.
 */
public final class LlmClientFactory {

    private final List<LlmProviderSpec> specs;
    private final Map<String, String> environment;

    /** @param environment usually {@code System.getenv()}; injected for tests */
    public LlmClientFactory(List<LlmProviderSpec> specs, Map<String, String> environment) {
        this.specs = List.copyOf(specs);
        this.environment = Map.copyOf(environment);
    }

    public LlmClient create(AiSettings settings) {
        Objects.requireNonNull(settings, "settings");
        List<LlmClient> clients = new ArrayList<>();
        for (AiProvider provider : settings.providerOrder()) {
            Optional<LlmProviderSpec> spec = specFor(provider);
            if (spec.isEmpty()) {
                continue;
            }
            Optional<String> key = settings.apiKey(provider).or(() -> variable(spec.get().keyVariable()));
            key.ifPresent(k -> clients.add(build(spec.get(), k)));
        }
        if (clients.isEmpty()) {
            return new UnavailableLlmClient();
        }
        LlmClient chain = clients.get(clients.size() - 1);
        for (int i = clients.size() - 2; i >= 0; i--) {
            chain = new FallbackLlmClient(clients.get(i), chain);
        }
        return chain;
    }

    /** @return a client for exactly this provider and key, with no fallback (for connection tests) */
    public LlmClient createFor(AiProvider provider, String apiKey) {
        LlmProviderSpec spec = specFor(provider)
                .orElseThrow(() -> new IllegalArgumentException("no client for provider " + provider));
        return build(spec, apiKey);
    }

    private LlmClient build(LlmProviderSpec spec, String apiKey) {
        return spec.maker().create(apiKey, variable(spec.modelVariable()).orElse(spec.defaultModel()));
    }

    private Optional<LlmProviderSpec> specFor(AiProvider provider) {
        return specs.stream().filter(spec -> spec.provider() == provider).findFirst();
    }

    private Optional<String> variable(String name) {
        String value = environment.get(name);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.strip());
    }
}
