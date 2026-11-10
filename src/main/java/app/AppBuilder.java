package app;

import data_access.circ.LogisimCircuitParser;
import data_access.file.TableFileStore;
import data_access.session.InMemorySessionStore;
import entity.simulation.CircuitSimulator;
import entity.truthtable.TruthTableChecker;
import entity.truthtable.TruthTableTextFormat;
import interface_adapter.check_circuit.CheckCircuitController;
import interface_adapter.check_circuit.CheckCircuitPresenter;
import interface_adapter.check_circuit.CheckCircuitViewModel;
import interface_adapter.import_circuit.ImportCircuitController;
import interface_adapter.import_circuit.ImportCircuitPresenter;
import interface_adapter.import_circuit.ImportCircuitViewModel;
import interface_adapter.table_file.TableFileController;
import interface_adapter.table_file.TableFilePresenter;
import interface_adapter.table_file.TableFileViewModel;
import use_case.check_circuit.CheckCircuitInteractor;
import use_case.import_circuit.ImportCircuitInteractor;
import use_case.table_file.TableFileInteractor;
import view.CircuitView;
import view.TruthTableView;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

/**
 * Creates every object the app needs and connects them. This is the only class that knows the
 * concrete classes of every layer; everything else receives its collaborators through its
 * constructor.
 *
 * <p>Each feature adds its tab in its own {@code addXxxTab()} method, so teammates can add
 * features without editing each other's code:
 * <pre>
 * new AppBuilder().addCircuitTab().addTutorTab().build().setVisible(true);
 * </pre>
 */
public final class AppBuilder {

    /** Shared by every feature: the open circuit and its latest check result. */
    private final InMemorySessionStore session = new InMemorySessionStore();
    private final TruthTableTextFormat tableFormat = new TruthTableTextFormat();
    private final JTabbedPane tabs = new JTabbedPane();

    /** Opening a circuit, checking it against a truth table, and saving / loading tables. */
    public AppBuilder addCircuitTab() {
        ImportCircuitViewModel circuitViewModel = new ImportCircuitViewModel();
        ImportCircuitController openController = new ImportCircuitController(new ImportCircuitInteractor(
                new LogisimCircuitParser(), session, new ImportCircuitPresenter(circuitViewModel)));

        CheckCircuitViewModel checkViewModel = new CheckCircuitViewModel();
        CheckCircuitController checkController = new CheckCircuitController(new CheckCircuitInteractor(
                session, tableFormat, new TruthTableChecker(new CircuitSimulator()),
                new CheckCircuitPresenter(checkViewModel)));

        TableFileViewModel tableFileViewModel = new TableFileViewModel();
        TableFileController tableFileController = new TableFileController(new TableFileInteractor(
                new TableFileStore(), tableFormat, new TableFilePresenter(tableFileViewModel)));

        CircuitView circuitView = new CircuitView(circuitViewModel, openController);
        TruthTableView tableView = new TruthTableView(checkViewModel, checkController, tableFileViewModel,
                tableFileController);
        circuitViewModel.addPropertyChangeListener(event -> {
            if (circuitViewModel.getState().getError().isEmpty()) {
                tableView.clearResults();
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, circuitView, tableView);
        split.setResizeWeight(0.55);
        tabs.addTab("Circuit", split);
        return this;
    }

    /** @return the main window, ready to show */
    public JFrame build() {
        JFrame frame = new JFrame("LogiCoach");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(tabs);
        frame.setSize(1100, 720);
        frame.setLocationRelativeTo(null);
        return frame;
    }
}
