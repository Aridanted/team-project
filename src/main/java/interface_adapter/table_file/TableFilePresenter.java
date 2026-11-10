package interface_adapter.table_file;

import use_case.table_file.TableFileOutputBoundary;
import use_case.table_file.TableFileOutputData;

import java.util.Objects;

/** Turns save and load results into messages for {@link TableFileState}. */
public final class TableFilePresenter implements TableFileOutputBoundary {

    private final TableFileViewModel viewModel;

    public TableFilePresenter(TableFileViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
    }

    @Override
    public void prepareSavedView(TableFileOutputData outputData) {
        TableFileState state = new TableFileState(viewModel.getState());
        state.setMessage("Saved " + fileName(outputData.path()) + ".");
        state.setError("");
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }

    @Override
    public void prepareLoadedView(TableFileOutputData outputData) {
        TableFileState state = new TableFileState(viewModel.getState());
        state.setTableText(outputData.tableText());
        state.setMessage("Loaded " + fileName(outputData.path()) + ".");
        state.setError("");
        viewModel.setState(state);
        viewModel.firePropertyChanged(TableFileViewModel.TABLE_LOADED);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        TableFileState state = new TableFileState(viewModel.getState());
        state.setMessage("");
        state.setError(errorMessage);
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }

    private static String fileName(String path) {
        return path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1);
    }
}
