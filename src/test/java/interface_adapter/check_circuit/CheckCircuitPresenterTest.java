package interface_adapter.check_circuit;

import org.junit.jupiter.api.Test;
import use_case.check_circuit.CheckCircuitInputData;
import use_case.check_circuit.CheckCircuitOutputData;
import use_case.check_circuit.CheckedRow;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckCircuitPresenterTest {

    private final CheckCircuitViewModel viewModel = new CheckCircuitViewModel();
    private final CheckCircuitPresenter presenter = new CheckCircuitPresenter(viewModel);
    private final List<Object> events = new ArrayList<>();

    private static CheckCircuitOutputData oneFailingRow() {
        CheckedRow pass = new CheckedRow(List.of("0", "0"), List.of("0", "0"), List.of("0", "0"), List.of());
        CheckedRow fail = new CheckedRow(List.of("1", "1"), List.of("1", "1"), List.of("0", "1"), List.of("Sum"));
        return new CheckCircuitOutputData(List.of("A", "B"), List.of("Sum", "Cout"), List.of(pass, fail),
                1, 2, List.of("Sum"));
    }

    @Test
    void showsRowsWithPassFailTextAndSummary() {
        viewModel.addPropertyChangeListener(event -> events.add(event.getNewValue()));
        presenter.prepareSuccessView(oneFailingRow());

        CheckCircuitState state = viewModel.getState();
        assertTrue(state.isChecked());
        assertEquals("", state.getError());
        assertEquals(List.of("A", "B"), state.getInputLabels());
        assertEquals("PASS", state.getRows().get(0).result());
        assertEquals("FAIL (Sum)", state.getRows().get(1).result());
        assertFalse(state.getRows().get(1).passed());
        assertEquals("1/2 rows pass. Failing outputs: Sum.", state.getSummary());
        assertEquals(1, events.size());
    }

    @Test
    void summaryOmitsFailingOutputsWhenAllPass() {
        CheckedRow pass = new CheckedRow(List.of("0"), List.of("1"), List.of("1"), List.of());
        presenter.prepareSuccessView(new CheckCircuitOutputData(List.of("A"), List.of("Y"), List.of(pass),
                1, 1, List.of()));
        assertEquals("1/1 rows pass.", viewModel.getState().getSummary());
    }

    @Test
    void failureKeepsPreviousResultsAndShowsError() {
        presenter.prepareSuccessView(oneFailingRow());
        presenter.prepareFailView("line 3: expected 2 output values, found 1");

        CheckCircuitState state = viewModel.getState();
        assertEquals("line 3: expected 2 output values, found 1", state.getError());
        assertEquals(2, state.getRows().size());
    }

    @Test
    void controllerPassesTheSpecThrough() {
        List<CheckCircuitInputData> received = new ArrayList<>();
        new CheckCircuitController(received::add).execute("inputs: A");
        assertEquals(List.of(new CheckCircuitInputData("inputs: A")), received);
    }
}
