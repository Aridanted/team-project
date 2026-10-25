package interface_adapter.check_circuit;

import java.util.List;

/** What the check screen shows. */
public final class CheckCircuitState {

    private List<String> inputLabels = List.of();
    private List<String> outputLabels = List.of();
    private List<ResultRow> rows = List.of();
    private String summary = "";
    private boolean checked;
    private String error = "";

    public CheckCircuitState() {
    }

    public CheckCircuitState(CheckCircuitState copy) {
        inputLabels = copy.inputLabels;
        outputLabels = copy.outputLabels;
        rows = copy.rows;
        summary = copy.summary;
        checked = copy.checked;
        error = copy.error;
    }

    public List<String> getInputLabels() {
        return inputLabels;
    }

    public void setInputLabels(List<String> inputLabels) {
        this.inputLabels = List.copyOf(inputLabels);
    }

    public List<String> getOutputLabels() {
        return outputLabels;
    }

    public void setOutputLabels(List<String> outputLabels) {
        this.outputLabels = List.copyOf(outputLabels);
    }

    public List<ResultRow> getRows() {
        return rows;
    }

    public void setRows(List<ResultRow> rows) {
        this.rows = List.copyOf(rows);
    }

    /** @return for example "3/4 rows pass. Failing outputs: Sum." */
    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    /** @return whether a check has produced results */
    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    /** @return the error to show, or "" if there is none */
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
