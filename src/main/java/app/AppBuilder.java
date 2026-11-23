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
import data_access.llm.builder.CircuitDesignPromptBuilder;
import data_access.llm.builder.LlmDesignGenerator;
import entity.design.CircuitDescriber;
import entity.settings.AiSettings;
import entity.simulation.CircuitSimulator;
import entity.truthtable.TruthTableChecker;
import entity.truthtable.TruthTableTextFormat;
import entity.expression.ExpressionParser;
import entity.truthtable.TruthTableGenerator;
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
import interface_adapter.build_circuit.BuildCircuitController;
import interface_adapter.build_circuit.BuildCircuitPresenter;
import interface_adapter.build_circuit.BuildCircuitViewModel;
import use_case.ai_settings.AiSettingsInteractor;
import use_case.ai_settings.SettingsStorageException;
import use_case.check_circuit.CheckCircuitInteractor;
import use_case.import_circuit.ImportCircuitInteractor;
import use_case.table_file.TableFileInteractor;
import use_case.tutor_chat.TutorChatInteractor;
import use_case.tutor_chat.TutorGateway;
import use_case.build_circuit.BuildCircuitInteractor;
import use_case.build_circuit.DesignAssembler;
import use_case.build_circuit.DesignValidator;
import use_case.build_circuit.OutputExpressionVerifier;
import view.AiSettingsView;
import view.CircuitView;
import view.MainFrame;
import view.OnEventThread;
import view.TruthTableView;
import view.TutorChatView;
import view.WorkspaceView;
import view.BuilderView;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Composition root: the only class that creates concrete classes from every layer. {@link #build()}
 * calls private {@code addXxxTab()} methods on a {@link MainFrame}; teammates replace placeholders
 * for Tutor and Builder without editing each other's tab code. The Workspace tab puts the circuit,
 * truth table and tutor views side by side, each still created by its own code here.
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
        addWorkspaceTab();
        addBuilderTab();
        addSettingsTab();
        return frame;
    }

    /** The circuit, its expected truth table and the tutor side by side (DD-14). */
    private void addWorkspaceTab() {
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

        frame.addTab("Workspace", new WorkspaceView(circuitView, tableView, tutorView()));
    }

    /** Person 3: the tutor chat, shown on the right of the Workspace tab. */
    private TutorChatView tutorView() {
        TutorChatViewModel viewModel = new TutorChatViewModel();
        TutorGateway tutor = new LlmTutorGateway(llmClient,
                new TutorPromptBuilder(new CircuitDescriber(), new CircuitJsonCodec()));
        TutorChatController controller = new TutorChatController(
                new TutorChatInteractor(session, tutor, new TutorChatPresenter(viewModel)));
        return new TutorChatView(controller, viewModel);
    }

    /** Creates the Builder tab using the shared AI client. */
    private void addBuilderTab() {
        BuildCircuitViewModel viewModel = new BuildCircuitViewModel();
        BuildCircuitPresenter presenter = new BuildCircuitPresenter(viewModel, tableFormat);

        LlmDesignGenerator generator = new LlmDesignGenerator(
                llmClient, new CircuitDesignPromptBuilder(), new CircuitJsonCodec());

        BuildCircuitInteractor interactor = new BuildCircuitInteractor(
                generator,
                new DesignValidator(),
                new DesignAssembler(),
                new TruthTableGenerator(new CircuitSimulator()),
                new OutputExpressionVerifier(new ExpressionParser()),
                presenter);

        BuildCircuitController controller = new BuildCircuitController(interactor);
        frame.addTab("Builder", new BuilderView(controller, viewModel));
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
