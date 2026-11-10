package interface_adapter.import_circuit;

import java.util.List;

/** Mutable state for the import-circuit screen. */
public class ImportCircuitState {

    private String circuitName = "";
    private String summary = "";
    private List<String> inputLabels = List.of();
    private List<String> outputLabels = List.of();
    private List<DrawableComponent> components = List.of();
    private List<DrawableWire> wires = List.of();
    private List<String> warnings = List.of();
    private boolean simulatable;
    private boolean loaded;
    private String error = "";

    public ImportCircuitState() {
    }

    /**
     * @param other state to copy
     */
    public ImportCircuitState(ImportCircuitState other) {
        this.circuitName = other.circuitName;
        this.summary = other.summary;
        this.inputLabels = other.inputLabels;
        this.outputLabels = other.outputLabels;
        this.components = other.components;
        this.wires = other.wires;
        this.warnings = other.warnings;
        this.simulatable = other.simulatable;
        this.loaded = other.loaded;
        this.error = other.error;
    }

    public String getCircuitName() {
        return circuitName;
    }

    public void setCircuitName(String circuitName) {
        this.circuitName = circuitName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public List<DrawableComponent> getComponents() {
        return components;
    }

    public void setComponents(List<DrawableComponent> components) {
        this.components = List.copyOf(components);
    }

    public List<DrawableWire> getWires() {
        return wires;
    }

    public void setWires(List<DrawableWire> wires) {
        this.wires = List.copyOf(wires);
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = List.copyOf(warnings);
    }

    public boolean isSimulatable() {
        return simulatable;
    }

    public void setSimulatable(boolean simulatable) {
        this.simulatable = simulatable;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
