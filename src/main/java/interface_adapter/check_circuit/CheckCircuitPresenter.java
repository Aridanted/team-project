package interface_adapter.check_circuit;

import use_case.check_circuit.CheckCircuitOutputBoundary;
import use_case.check_circuit.CheckCircuitOutputData;
import use_case.check_circuit.CheckedRow;

import java.util.Objects;

/** Turns check results into display text for {@link CheckCircuitState}. */
public final class CheckCircuitPresenter implements CheckCircuitOutputBoundary {

    private final CheckCircuitViewModel viewModel;

    public CheckCircuitPresenter(CheckCircuitViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
    }

    @Override
    public void prepareSuccessView(CheckCircuitOutputData outputData) {
        CheckCircuitState state = new CheckCircuitState(viewModel.getState());
        state.setInputLabels(outputData.inputLabels());
        state.setOutputLabels(outputData.outputLabels());
        state.setRows(outputData.rows().stream().map(CheckCircuitPresenter::toResultRow).toList());
        state.setSummary(summary(outputData));
        state.setChecked(true);
        state.setError("");
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        CheckCircuitState state = new CheckCircuitState(viewModel.getState());
        state.setError(errorMessage);
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }

    private static ResultRow toResultRow(CheckedRow row) {
        String result = row.passed() ? "PASS" : "FAIL (" + String.join(", ", row.failingOutputs()) + ")";
        return new ResultRow(row.inputs(), row.expected(), row.actual(), row.passed(), result);
    }

    private static String summary(CheckCircuitOutputData outputData) {
        String rows = outputData.passedRows() + "/" + outputData.totalRows() + " rows pass.";
        if (outputData.failingOutputs().isEmpty()) {
            return rows;
        }
        return rows + " Failing outputs: " + String.join(", ", outputData.failingOutputs()) + ".";
    }
}
