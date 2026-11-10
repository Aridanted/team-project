package data_access.llm;

import entity.settings.AiProvider;
import entity.settings.AiSettings;
import org.junit.jupiter.api.Test;
import use_case.ai_settings.AiConnectionException;
import use_case.ai_settings.ConnectionProblem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Factory, settings reloading and the connection tester, with fake provider clients. */
class LlmClientFactoryTest {

    private static final LlmRequest REQUEST = LlmRequest.single("", "hi");

    /** Records "provider:key:model" for every client built; each client replies with that text. */
    private final List<String> built = new ArrayList<>();

    private List<LlmProviderSpec> specs() {
        return List.of(spec(AiProvider.GEMINI, "GEMINI_API_KEY", "GEMINI_MODEL", "flash"),
                spec(AiProvider.GROQ, "GROQ_API_KEY", "GROQ_MODEL", "llama"));
    }

    private LlmProviderSpec spec(AiProvider provider, String keyVariable, String modelVariable, String model) {
        return new LlmProviderSpec(provider, keyVariable, modelVariable, model, (key, chosenModel) -> {
            String name = provider.id() + ":" + key + ":" + chosenModel;
            built.add(name);
            return FakeLlmClient.alwaysReplying(name);
        });
    }

    @Test
    void noKeysAnywhereGivesTheNoKeyClient() {
        LlmClient client = new LlmClientFactory(specs(), Map.of()).create(AiSettings.defaults());

        assertInstanceOf(UnavailableLlmClient.class, client);
    }

    @Test
    void savedKeyWinsOverEnvironmentAndModelsCanBeOverridden() throws LlmException {
        LlmClientFactory factory = new LlmClientFactory(specs(),
                Map.of("GEMINI_API_KEY", "envkey", "GEMINI_MODEL", " pro "));

        String reply = factory.create(new AiSettings(List.of(), Map.of(AiProvider.GEMINI, "saved"))).complete(REQUEST);

        assertEquals("gemini:saved:pro", reply);
    }

    @Test
    void environmentKeysAreUsedWhenNothingIsSaved() throws LlmException {
        LlmClientFactory factory = new LlmClientFactory(specs(), Map.of("GROQ_API_KEY", "gsk_env", "GEMINI_API_KEY", " "));

        assertEquals("groq:gsk_env:llama", factory.create(AiSettings.defaults()).complete(REQUEST));
    }

    @Test
    void providersAreChainedInTheSavedOrder() throws LlmException {
        LlmClientFactory factory = new LlmClientFactory(specs(), Map.of());
        AiSettings groqFirst = new AiSettings(List.of(AiProvider.GROQ),
                Map.of(AiProvider.GEMINI, "g", AiProvider.GROQ, "q"));

        LlmClient client = factory.create(groqFirst);

        assertInstanceOf(FallbackLlmClient.class, client);
        assertEquals("groq:q:llama", client.complete(REQUEST));
        assertEquals(List.of("groq:q:llama", "gemini:g:flash"), built);
    }

    @Test
    void settingsBackedClientRebuildsOnlyWhenSettingsChange() throws LlmException {
        AtomicReference<AiSettings> settings = new AtomicReference<>(
                new AiSettings(List.of(), Map.of(AiProvider.GEMINI, "one")));
        SettingsBackedLlmClient client = new SettingsBackedLlmClient(settings::get,
                new LlmClientFactory(specs(), Map.of()));

        assertEquals("gemini:one:flash", client.complete(REQUEST));
        assertEquals("gemini:one:flash", client.complete(REQUEST));
        settings.set(new AiSettings(List.of(), Map.of(AiProvider.GEMINI, "two")));
        assertEquals("gemini:two:flash", client.complete(REQUEST));

        assertEquals(List.of("gemini:one:flash", "gemini:two:flash"), built);
    }

    @Test
    void connectionTesterUsesOnlyTheGivenProviderAndKey() throws AiConnectionException {
        LlmClientFactory factory = new LlmClientFactory(specs(), Map.of("GEMINI_API_KEY", "env"));

        new LlmConnectionTester(factory).test(AiProvider.GROQ, "typed");

        assertEquals(List.of("groq:typed:llama"), built);
    }

    @Test
    void connectionTesterTranslatesFailures() {
        LlmClientFactory factory = new LlmClientFactory(List.of(new LlmProviderSpec(AiProvider.GEMINI, "K", "M", "m",
                (key, model) -> FakeLlmClient.alwaysFailing(LlmException.Kind.AUTH))), Map.of());

        AiConnectionException e = assertThrows(AiConnectionException.class,
                () -> new LlmConnectionTester(factory).test(AiProvider.GEMINI, "bad"));

        assertEquals(ConnectionProblem.AUTH, e.problem());
        for (LlmException.Kind kind : LlmException.Kind.values()) {
            assertEquals(kind.name(), LlmConnectionTester.problemFor(kind).name());
        }
    }

    @Test
    void standardProvidersBuildRealClients() {
        LlmClientFactory factory = new LlmClientFactory(LlmProviders.standard(FakeHttpTransport.answering(200, "")),
                Map.of());

        assertInstanceOf(GeminiClient.class, factory.createFor(AiProvider.GEMINI, "k"));
        assertInstanceOf(GroqClient.class, factory.createFor(AiProvider.GROQ, "k"));
    }
}
