package interface_adapter.save_built_circuit;

import use_case.save_built_circuit.SaveBuiltCircuitOutputBoundary;

import java.util.Objects;

/** Converts save outcomes into messages for the Builder. */
public final class SaveBuiltCircuitPresenter implements SaveBuiltCircuitOutputBoundary {
    private final SaveBuiltCircuitViewModel viewModel;

    /**
     * Creates the save presenter.
     *
     * @param viewModel the source of save messages for the Builder
     */
    public SaveBuiltCircuitPresenter(SaveBuiltCircuitViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
    }

    @Override
    public void prepareSuccessView(String path) {
        publish(new SaveBuiltCircuitState(
                "Saved and verified: " + path, ""));
    }

    @Override
    public void prepareFailView(String message) {
        publish(new SaveBuiltCircuitState("", message));
    }

    private void publish(SaveBuiltCircuitState state) {
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }
}
