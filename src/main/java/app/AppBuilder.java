package app;

import data_access.circ.LogisimCircuitParser;
import data_access.json.JsonSettingsStore;
import data_access.llm.JdkHttpTransport;
import data_access.llm.LlmClient;
import data_access.llm.LlmClientFactory;
import data_access.llm.LlmConnectionTester;
import data_access.llm.LlmProviders;
import data_access.llm.RateLimitedLlmClient;
import data_access.llm.SettingsBackedLlmClient;
import data_access.session.InMemorySessionStore;
import entity.settings.AiSettings;
import entity.simulation.CircuitSimulator;
import entity.truthtable.TruthTableChecker;
import entity.truthtable.TruthTableTextFormat;
import interface_adapter.ai_settings.AiSettingsController;
import interface_adapter.ai_settings.AiSettingsPresenter;
import interface_adapter.ai_settings.AiSettingsViewModel;
import interface_adapter.check_circuit.CheckCircuitController;
import interface_adapter.check_circuit.CheckCircuitPresenter;
import interface_adapter.check_circuit.CheckCircuitViewModel;
import interface_adapter.import_circuit.ImportCircuitController;
import interface_adapter.import_circuit.ImportCircuitPresenter;
import interface_adapter.import_circuit.ImportCircuitViewModel;
import use_case.ai_settings.AiSettingsInteractor;
import use_case.ai_settings.SettingsStorageException;
import use_case.check_circuit.CheckCircuitInteractor;
import use_case.import_circuit.ImportCircuitInteractor;
import view.AiSettingsView;
import view.CircuitView;
import view.MainFrame;
import view.PlaceholderView;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Composition root: the only class that creates concrete classes from every layer. Each feature
 * has its own private method so teammates add their tab without merge conflicts.
 */
public final class AppBuilder {

    static final String DATA_DIR_VARIABLE = "LOGICOACH_DATA_DIR";
    static final Duration MIN_AI_CALL_INTERVAL = Duration.ofSeconds(2);

    private final Map<String, String> environment;
    private final MainFrame frame = new MainFrame();
    private final InMemorySessionStore session = new InMemorySessionStore();
    private final JsonSettingsStore settingsStore;
    private final LlmClientFactory llmClientFactory;

    /** The AI client for the tutor and the builder: follows saved settings, falls back, rate limited. */
    private final LlmClient llmClient;

    /** @param environment usually {@code System.getenv()} */
    public AppBuilder(Map<String, String> environment) {
        this.environment = Map.copyOf(environment);
        this.settingsStore = new JsonSettingsStore(dataDirectory().resolve("settings.json"));
        this.llmClientFactory = new LlmClientFactory(LlmProviders.standard(new JdkHttpTransport()), this.environment);
        this.llmClient = new RateLimitedLlmClient(
                new SettingsBackedLlmClient(this::currentSettings, llmClientFactory), MIN_AI_CALL_INTERVAL);
    }

    public MainFrame build() {
        addCircuitTab();
        addTutorTab();
        addBuilderTab();
        addSettingsTab();
        return frame;
    }

    private void addCircuitTab() {
        ImportCircuitViewModel importViewModel = new ImportCircuitViewModel();
        ImportCircuitController importController = new ImportCircuitController(new ImportCircuitInteractor(
                new LogisimCircuitParser(), session, new ImportCircuitPresenter(importViewModel)));

        CheckCircuitViewModel checkViewModel = new CheckCircuitViewModel();
        CheckCircuitController checkController = new CheckCircuitController(new CheckCircuitInteractor(
                session, new TruthTableTextFormat(), new TruthTableChecker(new CircuitSimulator()),
                new CheckCircuitPresenter(checkViewModel)));

        frame.addTab("Circuit", new CircuitView(importController, importViewModel, checkController, checkViewModel));
    }

    /** Person 3: replace the placeholder with the tutor view, built on {@link #llmClient} and {@link #session}. */
    private void addTutorTab() {
        frame.addTab("Tutor", new PlaceholderView("The AI tutor chat will appear here."));
    }

    /** Person 4: replace the placeholder with the builder view, built on {@link #llmClient}. */
    private void addBuilderTab() {
        frame.addTab("Builder", new PlaceholderView("The circuit builder (for practice and learning) will appear here."));
    }

    private void addSettingsTab() {
        AiSettingsViewModel viewModel = new AiSettingsViewModel();
        AiSettingsController controller = new AiSettingsController(new AiSettingsInteractor(
                settingsStore, new LlmConnectionTester(llmClientFactory), new AiSettingsPresenter(viewModel)));
        frame.addTab("Settings", new AiSettingsView(controller, viewModel));
        controller.load();
    }

    private Path dataDirectory() {
        String configured = environment.get(DATA_DIR_VARIABLE);
        return configured == null || configured.isBlank() ? Path.of("data") : Path.of(configured.strip());
    }

    /** Unreadable settings count as none, so AI features fall back to offline instead of crashing. */
    private AiSettings currentSettings() {
        try {
            return settingsStore.load();
        } catch (SettingsStorageException e) {
            return AiSettings.defaults();
        }
    }
}
