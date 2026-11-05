package use_case.ai_settings;

import entity.settings.AiProvider;
import entity.settings.AiSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSettingsInteractorTest {

    private FakeStore store;
    private FakeTester tester;
    private RecordingPresenter presenter;
    private AiSettingsInteractor interactor;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        tester = new FakeTester();
        presenter = new RecordingPresenter();
        interactor = new AiSettingsInteractor(store, tester, presenter);
    }

    @Test
    void loadPresentsSavedSettingsInOrder() {
        store.saved = new AiSettings(List.of(AiProvider.GROQ), Map.of(AiProvider.GROQ, "gsk_1"));

        interactor.load();

        AiSettingsOutputData data = presenter.settings.get(0);
        assertFalse(data.saved());
        assertEquals(List.of(new ProviderSettings("groq", "Groq", "gsk_1"), new ProviderSettings("gemini", "Gemini", "")),
                data.providers());
        assertTrue(presenter.errors.isEmpty());
    }

    @Test
    void unreadableSettingsShowDefaultsAndTheError() {
        store.loadFailure = new SettingsStorageException("The settings file is damaged.", null);

        interactor.load();

        assertEquals("gemini", presenter.settings.get(0).providers().get(0).id());
        assertEquals(List.of("The settings file is damaged."), presenter.errors);
    }

    @Test
    void saveStoresTrimmedKeysAndPresentsThem() {
        interactor.save(new SaveAiSettingsInputData(List.of("groq", "gemini"),
                Map.of("gemini", "  AIza1  ", "groq", "")));

        assertEquals(new AiSettings(List.of(AiProvider.GROQ, AiProvider.GEMINI), Map.of(AiProvider.GEMINI, "AIza1")),
                store.saved);
        AiSettingsOutputData data = presenter.settings.get(0);
        assertTrue(data.saved());
        assertEquals("AIza1", data.providers().get(1).apiKey());
    }

    @Test
    void saveRejectsBadInputWithoutWriting() {
        interactor.save(new SaveAiSettingsInputData(List.of("openai"), Map.of()));
        interactor.save(new SaveAiSettingsInputData(List.of(), Map.of("gemini", "two words")));

        assertNull(store.saved);
        assertEquals("Unknown AI provider \"openai\".", presenter.errors.get(0));
        assertTrue(presenter.errors.get(1).startsWith("The Gemini API key contains spaces"));
    }

    @Test
    void saveReportsStorageErrors() {
        store.saveFailure = new SettingsStorageException("Could not save the settings.", null);

        interactor.save(new SaveAiSettingsInputData(List.of(), Map.of("gemini", "k")));

        assertEquals(List.of("Could not save the settings."), presenter.errors);
        assertTrue(presenter.settings.isEmpty());
    }

    @Test
    void connectionTestUsesTheTypedKey() {
        interactor.testConnection(new TestConnectionInputData("gemini", " AIza-typed "));

        assertEquals(List.of("GEMINI:AIza-typed"), tester.calls);
        assertEquals(ConnectionTestOutputData.connected("Gemini"), presenter.connections.get(0));
    }

    @Test
    void connectionProblemsArePresented() {
        tester.failure = new AiConnectionException(ConnectionProblem.RATE_LIMIT, "Groq: HTTP 429");

        interactor.testConnection(new TestConnectionInputData("groq", "gsk_x"));

        ConnectionTestOutputData data = presenter.connections.get(0);
        assertFalse(data.isConnected());
        assertEquals(ConnectionProblem.RATE_LIMIT, data.problem());
        assertEquals("Groq", data.providerName());
    }

    @Test
    void connectionTestNeedsAKnownProviderAndAPlausibleKey() {
        interactor.testConnection(new TestConnectionInputData("gemini", "  "));
        interactor.testConnection(new TestConnectionInputData("gemini", null));
        interactor.testConnection(new TestConnectionInputData("nope", "k"));
        interactor.testConnection(new TestConnectionInputData("groq", "gsk 1"));

        assertTrue(tester.calls.isEmpty());
        assertEquals("Enter a Gemini API key first.", presenter.errors.get(0));
        assertEquals("Enter a Gemini API key first.", presenter.errors.get(1));
        assertEquals("Unknown AI provider \"nope\".", presenter.errors.get(2));
        assertTrue(presenter.errors.get(3).startsWith("The Groq API key contains spaces"));
    }

    private static final class FakeStore implements AiSettingsDataAccessInterface {
        AiSettings saved;
        SettingsStorageException loadFailure;
        SettingsStorageException saveFailure;

        @Override
        public AiSettings load() throws SettingsStorageException {
            if (loadFailure != null) {
                throw loadFailure;
            }
            return saved == null ? AiSettings.defaults() : saved;
        }

        @Override
        public void save(AiSettings settings) throws SettingsStorageException {
            if (saveFailure != null) {
                throw saveFailure;
            }
            saved = settings;
        }
    }

    private static final class FakeTester implements AiConnectionTester {
        final List<String> calls = new ArrayList<>();
        AiConnectionException failure;

        @Override
        public void test(AiProvider provider, String apiKey) throws AiConnectionException {
            calls.add(provider + ":" + apiKey);
            if (failure != null) {
                throw failure;
            }
        }
    }

    private static final class RecordingPresenter implements AiSettingsOutputBoundary {
        final List<AiSettingsOutputData> settings = new ArrayList<>();
        final List<ConnectionTestOutputData> connections = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        @Override
        public void prepareSettingsView(AiSettingsOutputData outputData) {
            settings.add(outputData);
        }

        @Override
        public void prepareConnectionView(ConnectionTestOutputData outputData) {
            connections.add(outputData);
        }

        @Override
        public void prepareFailView(String errorMessage) {
            errors.add(errorMessage);
        }
    }
}
