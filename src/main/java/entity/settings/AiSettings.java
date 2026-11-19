package entity.settings;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The user's AI settings: which provider to try first, and an API key per provider.
 *
 * <p>Normalised on construction: the order lists every provider exactly once (missing ones are
 * appended in declaration order), keys are stripped, and blank keys mean "no key". A key that
 * contains whitespace or is implausibly long is rejected, since it was almost certainly pasted
 * wrong.
 *
 * @param providerOrder providers in the order they are tried
 * @param apiKeys       the API key of each provider that has one
 */
public record AiSettings(List<AiProvider> providerOrder, Map<AiProvider, String> apiKeys) {

    public static final int MAX_KEY_LENGTH = 200;

    public AiSettings {
        providerOrder = completeOrder(Objects.requireNonNull(providerOrder, "providerOrder"));
        apiKeys = cleanKeys(Objects.requireNonNull(apiKeys, "apiKeys"));
    }

    /** @return settings with no keys and providers in declaration order (Gemini first) */
    public static AiSettings defaults() {
        return new AiSettings(List.of(), Map.of());
    }

    /** @return the key for {@code provider}, if one is set */
    public Optional<String> apiKey(AiProvider provider) {
        return Optional.ofNullable(apiKeys.get(provider));
    }

    private static List<AiProvider> completeOrder(List<AiProvider> order) {
        Set<AiProvider> complete = new LinkedHashSet<>();
        for (AiProvider provider : order) {
            complete.add(Objects.requireNonNull(provider, "provider"));
        }
        Collections.addAll(complete, AiProvider.values());
        return List.copyOf(complete);
    }

    private static Map<AiProvider, String> cleanKeys(Map<AiProvider, String> keys) {
        Map<AiProvider, String> clean = new EnumMap<>(AiProvider.class);
        for (Map.Entry<AiProvider, String> entry : keys.entrySet()) {
            AiProvider provider = Objects.requireNonNull(entry.getKey(), "provider");
            String key = entry.getValue() == null ? "" : entry.getValue().strip();
            if (key.isEmpty()) {
                continue;
            }
            requirePlausibleKey(provider, key);
            clean.put(provider, key);
        }
        return Collections.unmodifiableMap(clean);
    }

    private static void requirePlausibleKey(AiProvider provider, String key) {
        if (key.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("The " + provider.displayName()
                    + " API key contains spaces or line breaks. Copy it again from the provider's website.");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("The " + provider.displayName() + " API key is longer than "
                    + MAX_KEY_LENGTH + " characters. Check that only the key was pasted.");
        }
    }

    /** Never prints the keys themselves, so settings can appear in logs and test failures. */
    @Override
    public String toString() {
        return "AiSettings[providerOrder=" + providerOrder + ", keysSetFor=" + apiKeys.keySet() + "]";
    }
}
