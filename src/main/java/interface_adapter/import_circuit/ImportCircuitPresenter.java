package interface_adapter.import_circuit;

import use_case.import_circuit.ComponentSummary;
import use_case.import_circuit.ImportCircuitOutputBoundary;
import use_case.import_circuit.ImportCircuitOutputData;
import use_case.import_circuit.WireSummary;

import java.util.List;
import java.util.Objects;

/** Maps import-circuit output data into {@link ImportCircuitState} for the view. */
public final class ImportCircuitPresenter implements ImportCircuitOutputBoundary {

    private final ImportCircuitViewModel viewModel;

    /**
     * @param viewModel state holder for the import-circuit screen
     */
    public ImportCircuitPresenter(ImportCircuitViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
    }

    @Override
    public void prepareSuccessView(ImportCircuitOutputData outputData) {
        ImportCircuitState state = new ImportCircuitState(viewModel.getState());
        state.setCircuitName(outputData.circuitName());
        state.setInputLabels(outputData.inputLabels());
        state.setOutputLabels(outputData.outputLabels());
        state.setComponents(mapComponents(outputData.components()));
        state.setWires(mapWires(outputData.wires()));
        state.setWarnings(outputData.warnings());
        state.setSimulatable(outputData.simulatable());
        state.setSummary(buildSummary(outputData.components()));
        state.setLoaded(true);
        state.setError("");
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        ImportCircuitState state = new ImportCircuitState(viewModel.getState());
        state.setError(errorMessage);
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }

    private static List<DrawableComponent> mapComponents(List<ComponentSummary> components) {
        return components.stream()
                .map(c -> new DrawableComponent(
                        c.kind(), c.detail(), c.label(), c.x(), c.y(), c.facing()))
                .toList();
    }

    private static List<DrawableWire> mapWires(List<WireSummary> wires) {
        return wires.stream()
                .map(w -> new DrawableWire(w.x1(), w.y1(), w.x2(), w.y2()))
                .toList();
    }

    private static String buildSummary(List<ComponentSummary> components) {
        int inputs = countKind(components, "input");
        int outputs = countKind(components, "output");
        int gates = countKind(components, "gate");
        return formatCount(inputs, "input", "inputs")
                + ", "
                + formatCount(outputs, "output", "outputs")
                + ", "
                + formatCount(gates, "gate", "gates");
    }

    private static int countKind(List<ComponentSummary> components, String kind) {
        return (int) components.stream().filter(c -> kind.equals(c.kind())).count();
    }

    private static String formatCount(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }
}
