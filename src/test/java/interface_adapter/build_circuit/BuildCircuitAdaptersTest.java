package interface_adapter.build_circuit;

import entity.design.CircuitDesign;
import entity.simulation.Logic;
import entity.truthtable.TruthTable;
import entity.truthtable.TruthTableTextFormat;
import org.junit.jupiter.api.Test;
import use_case.build_circuit.BuildCircuitInputData;
import use_case.build_circuit.BuildCircuitOutputData;
import use_case.build_circuit.DesignAssembler;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildCircuitAdaptersTest {

    @Test
    void controllerForwardsTheDescriptionToTheInputBoundary() {
        List<BuildCircuitInputData> requests = new ArrayList<>();
        BuildCircuitController controller = new BuildCircuitController(requests::add);

        controller.build("Connect A to Y");

        assertEquals(List.of(new BuildCircuitInputData("Connect A to Y")), requests);
    }

    @Test
    void presenterPublishesTheCircuitNameAndFormattedTruthTable() {
        BuildCircuitViewModel viewModel = new BuildCircuitViewModel();
        BuildCircuitPresenter presenter = new BuildCircuitPresenter(viewModel, new TruthTableTextFormat());
        List<PropertyChangeEvent> events = new ArrayList<>();
        viewModel.addPropertyChangeListener(events::add);

        presenter.prepareSuccessView(passThroughResult());

        BuildCircuitState expected = new BuildCircuitState(
                "Pass through", "inputs: A\noutputs: Y\n0 | 0\n1 | 1\n", "");
        assertEquals(expected, viewModel.getState());
        assertEquals(1, events.size());
        assertEquals("state", events.getFirst().getPropertyName());
        assertEquals(expected, events.getFirst().getNewValue());
    }

    @Test
    void presenterClearsThePreviousResultAndPublishesTheError() {
        BuildCircuitViewModel viewModel = new BuildCircuitViewModel();
        BuildCircuitPresenter presenter = new BuildCircuitPresenter(viewModel, new TruthTableTextFormat());
        presenter.prepareSuccessView(passThroughResult());

        List<PropertyChangeEvent> events = new ArrayList<>();
        viewModel.addPropertyChangeListener(events::add);
        presenter.prepareFailView("Could not build a circuit.");

        BuildCircuitState expected = new BuildCircuitState("", "", "Could not build a circuit.");
        assertEquals(expected, viewModel.getState());
        assertEquals(1, events.size());
        assertEquals(expected, events.getFirst().getNewValue());
    }

    private static BuildCircuitOutputData passThroughResult() {
        CircuitDesign design = new CircuitDesign("Pass through", List.of("A"), List.of("Y"),
                List.of(), Map.of("Y", "A"));
        TruthTable table = new TruthTable(List.of("A"), List.of("Y"), List.of(
                new TruthTable.Row(List.of(Logic.ZERO), List.of(Logic.ZERO)),
                new TruthTable.Row(List.of(Logic.ONE), List.of(Logic.ONE))));

        return new BuildCircuitOutputData(design, new DesignAssembler().assemble(design), table);
    }
}
