package app;

import data_access.circ.LogisimCircuitParser;
import data_access.file.TableFileStore;
import data_access.json.CircuitJsonCodec;
import data_access.json.JsonSettingsStore;
import data_access.llm.JdkHttpTransport;
import data_access.llm.LlmClient;
import data_access.llm.LlmClientFactory;
import data_access.llm.LlmConnectionTester;
import data_access.llm.LlmProviders;
import data_access.llm.RateLimitedLlmClient;
import data_access.llm.SettingsBackedLlmClient;
import data_access.llm.tutor.LlmTutorGateway;
import data_access.llm.tutor.TutorPromptBuilder;
import data_access.session.InMemorySessionStore;
import entity.design.CircuitDescriber;
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
import interface_adapter.check_circuit.SpecTemplate;
import interface_adapter.import_circuit.ImportCircuitController;
import interface_adapter.import_circuit.ImportCircuitPresenter;
import interface_adapter.import_circuit.ImportCircuitViewModel;
import interface_adapter.table_file.TableFileController;
import interface_adapter.table_file.TableFilePresenter;
import interface_adapter.table_file.TableFileViewModel;
import interface_adapter.tutor_chat.TutorChatController;
import interface_adapter.tutor_chat.TutorChatPresenter;
import interface_adapter.tutor_chat.TutorChatViewModel;
import use_case.ai_settings.AiSettingsInteractor;
import use_case.ai_settings.SettingsStorageException;
import use_case.check_circuit.CheckCircuitInteractor;
import use_case.import_circuit.ImportCircuitInteractor;
import use_case.table_file.TableFileInteractor;
import use_case.tutor_chat.TutorChatInteractor;
import use_case.tutor_chat.TutorGateway;
import view.AiSettingsView;
import view.CircuitView;
import view.MainFrame;
import view.OnEventThread;
import view.PlaceholderView;
import view.TruthTableView;
import view.TutorChatView;

import javax.swing.JSplitPane;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Composition root: the only class that creates concrete classes from every layer. {@link #build()}
 * calls private {@code addXxxTab()} methods on a {@link MainFrame}; teammates replace placeholders
 * for Tutor and Builder without editing each other's tab code.
 */
public final class AppBuilder {

    static final String DATA_DIR_VARIABLE = "LOGICOACH_DATA_DIR";
    static final Duration MIN_AI_CALL_INTERVAL = Duration.ofSeconds(2);

    private final Map<String, String> environment;
    private final MainFrame frame = new MainFrame();
    private final InMemorySessionStore session = new InMemorySessionStore();
    private final TruthTableTextFormat tableFormat = new TruthTableTextFormat();
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
                session, tableFormat, new TruthTableChecker(new CircuitSimulator()),
                new CheckCircuitPresenter(checkViewModel)));

        TableFileViewModel tableFileViewModel = new TableFileViewModel();
        TableFileController tableFileController = new TableFileController(new TableFileInteractor(
                new TableFileStore(), tableFormat, new TableFilePresenter(tableFileViewModel)));

        CircuitView circuitView = new CircuitView(importViewModel, importController);
        TruthTableView tableView = new TruthTableView(checkViewModel, checkController, tableFileViewModel,
                tableFileController);

        importViewModel.addPropertyChangeListener(OnEventThread.listener(event -> {
            var state = importViewModel.getState();
            if (state.getError().isEmpty()) {
                tableView.clearResults();
                if (state.isLoaded()) {
                    tableView.offerTemplate(SpecTemplate.forLabels(state.getInputLabels(), state.getOutputLabels()));
                }
            }
        }));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, circuitView, tableView);
        split.setResizeWeight(0.55);
        frame.addTab("Circuit", split);
    }

    private void addTutorTab() {
        TutorChatViewModel viewModel = new TutorChatViewModel();
        TutorGateway tutor = new LlmTutorGateway(llmClient,
                new TutorPromptBuilder(new CircuitDescriber(), new CircuitJsonCodec()));
        TutorChatController controller = new TutorChatController(
                new TutorChatInteractor(session, tutor, new TutorChatPresenter(viewModel)));
        frame.addTab("Tutor", new TutorChatView(controller, viewModel));
    }

    /** Person 4: replace the placeholder with the builder view, built on {@link #llmClient}. */
    private void addBuilderTab() {
        String message = "The circuit builder (for practice and learning) will appear here.";
        frame.addTab("Builder", new PlaceholderView(message));
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
