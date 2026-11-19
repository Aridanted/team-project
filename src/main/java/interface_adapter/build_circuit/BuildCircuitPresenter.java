package interface_adapter.build_circuit;

import entity.truthtable.TruthTableTextFormat;
import use_case.build_circuit.BuildCircuitOutputBoundary;
import use_case.build_circuit.BuildCircuitOutputData;

import java.util.Objects;

/** Converts build outcomes into state the Builder can display. */
public final class BuildCircuitPresenter implements BuildCircuitOutputBoundary {
    private final BuildCircuitViewModel viewModel;
    private final TruthTableTextFormat textFormat;

    /**
     * Creates the presenter with its view model and truth table formatter.
     *
     * @param viewModel the Builder view model to update
     * @param textFormat the formatter for simulated truth tables
     */
    public BuildCircuitPresenter(BuildCircuitViewModel viewModel, TruthTableTextFormat textFormat) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.textFormat = Objects.requireNonNull(textFormat, "textFormat");
    }

    @Override
    public void prepareSuccessView(BuildCircuitOutputData outputData) {
        String tableText = textFormat.format(outputData.truthTable());
        publish(new BuildCircuitState(outputData.circuit().name(), tableText, "",
                outputData.explanation()));
    }

    @Override
    public void prepareFailView(String errorMessage) {
        publish(new BuildCircuitState("", "", errorMessage));
    }

    private void publish(BuildCircuitState state) {
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }
}
