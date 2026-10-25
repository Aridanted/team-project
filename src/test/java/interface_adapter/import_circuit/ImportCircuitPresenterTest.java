package interface_adapter.import_circuit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import use_case.import_circuit.ComponentSummary;
import use_case.import_circuit.ImportCircuitOutputData;
import use_case.import_circuit.WireSummary;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportCircuitPresenterTest {

    private ImportCircuitViewModel viewModel;
    private ImportCircuitPresenter presenter;
    private List<PropertyChangeEvent> events;

    @BeforeEach
    void setUp() {
        viewModel = new ImportCircuitViewModel();
        presenter = new ImportCircuitPresenter(viewModel);
        events = new ArrayList<>();
        viewModel.addPropertyChangeListener(events::add);
    }

    @Test
    void successMapsOutputDataAndBuildsSummaryWithPluralForms() {
        List<ComponentSummary> components = List.of(
                new ComponentSummary("input", "", "A", 0, 0, "east"),
                new ComponentSummary("input", "", "B", 0, 10, "east"),
                new ComponentSummary("gate", "XOR", "", 0, 20, "east"),
                new ComponentSummary("gate", "AND", "", 0, 30, "east"),
                new ComponentSummary("output", "", "Sum", 0, 40, "west"),
                new ComponentSummary("output", "", "Cout", 0, 50, "west"));
        ImportCircuitOutputData outputData = new ImportCircuitOutputData(
                "half adder",
                List.of("A", "B"),
                List.of("Cout", "Sum"),
                components,
                List.of(new WireSummary(1, 2, 3, 4)),
                List.of("warn"),
                true);

        presenter.prepareSuccessView(outputData);

        ImportCircuitState state = viewModel.getState();
        assertEquals("half adder", state.getCircuitName());
        assertEquals("2 inputs, 2 outputs, 2 gates", state.getSummary());
        assertEquals(List.of("A", "B"), state.getInputLabels());
        assertEquals(List.of("Cout", "Sum"), state.getOutputLabels());
        assertEquals(List.of("warn"), state.getWarnings());
        assertTrue(state.isSimulatable());
        assertTrue(state.isLoaded());
        assertEquals("", state.getError());
        assertEquals(6, state.getComponents().size());
        DrawableComponent gate = state.getComponents().get(2);
        assertEquals("gate", gate.kind());
        assertEquals("XOR", gate.detail());
        assertEquals(1, state.getWires().size());
        assertEquals(new DrawableWire(1, 2, 3, 4), state.getWires().get(0));
        assertEquals(1, events.size());
        assertEquals("state", events.get(0).getPropertyName());
    }

    @Test
    void successSummaryUsesSingularForms() {
        ImportCircuitOutputData outputData = new ImportCircuitOutputData(
                "tiny",
                List.of("A"),
                List.of("Y"),
                List.of(
                        new ComponentSummary("input", "", "A", 0, 0, "east"),
                        new ComponentSummary("gate", "NOT", "", 0, 10, "east"),
                        new ComponentSummary("output", "", "Y", 0, 20, "west")),
                List.of(),
                List.of(),
                true);

        presenter.prepareSuccessView(outputData);

        assertEquals("1 input, 1 output, 1 gate", viewModel.getState().getSummary());
    }

    @Test
    void failureKeepsPreviousCircuitAndSetsError() {
        ImportCircuitOutputData outputData = new ImportCircuitOutputData(
                "saved",
                List.of("A"),
                List.of("Y"),
                List.of(new ComponentSummary("input", "", "A", 0, 0, "east")),
                List.of(),
                List.of(),
                true);
        presenter.prepareSuccessView(outputData);
        events.clear();

        presenter.prepareFailView("could not read file");

        ImportCircuitState state = viewModel.getState();
        assertEquals("could not read file", state.getError());
        assertEquals("saved", state.getCircuitName());
        assertEquals(List.of("A"), state.getInputLabels());
        assertTrue(state.isLoaded());
        assertEquals(1, events.size());
    }

    @Test
    void failureFromInitialStateStillFiresPropertyChange() {
        presenter.prepareFailView("no file");

        assertEquals("no file", viewModel.getState().getError());
        assertFalse(viewModel.getState().isLoaded());
        assertEquals(1, events.size());
    }
}
