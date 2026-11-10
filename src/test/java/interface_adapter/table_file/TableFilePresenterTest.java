package interface_adapter.table_file;

import org.junit.jupiter.api.Test;
import use_case.table_file.TableFileOutputData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableFilePresenterTest {

    private final TableFileViewModel viewModel = new TableFileViewModel();
    private final TableFilePresenter presenter = new TableFilePresenter(viewModel);
    private final List<String> firedProperties = new ArrayList<>();

    TableFilePresenterTest() {
        viewModel.addPropertyChangeListener(event -> firedProperties.add(event.getPropertyName()));
    }

    @Test
    void loadedTableReplacesTheEditorText() {
        presenter.prepareLoadedView(new TableFileOutputData("/home/me/tables/and.txt", "inputs: A\n"));

        assertEquals("inputs: A\n", viewModel.getState().getTableText());
        assertEquals("Loaded and.txt.", viewModel.getState().getMessage());
        assertEquals(List.of(TableFileViewModel.TABLE_LOADED), firedProperties);
    }

    @Test
    void savingOnlyShowsAMessage() {
        presenter.prepareSavedView(new TableFileOutputData("C:\\tables\\or.txt", "inputs: A\n"));

        assertEquals("", viewModel.getState().getTableText());
        assertEquals("Saved or.txt.", viewModel.getState().getMessage());
        assertEquals(List.of("state"), firedProperties);
    }

    @Test
    void failureClearsTheMessageAndShowsTheError() {
        presenter.prepareSavedView(new TableFileOutputData("or.txt", "x"));
        presenter.prepareFailView("Could not save to /x.txt.");

        assertEquals("", viewModel.getState().getMessage());
        assertEquals("Could not save to /x.txt.", viewModel.getState().getError());
    }
}
