package interface_adapter.build_circuit;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;
import entity.circuit.UnsupportedComponent;
import interface_adapter.import_circuit.DrawableComponent;
import interface_adapter.import_circuit.DrawableWire;
import entity.truthtable.TruthTableTextFormat;
import use_case.build_circuit.BuildCircuitOutputBoundary;
import use_case.build_circuit.BuildCircuitOutputData;
import use_case.build_circuit.CircuitPreviewLayout;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Converts build outcomes into state the Builder can display. */
public final class BuildCircuitPresenter implements BuildCircuitOutputBoundary {
    private final BuildCircuitViewModel viewModel;
    private final TruthTableTextFormat textFormat;
    private final CircuitPreviewLayout previewLayout;

    /**
     * Creates a presenter using existing circuit positions.
     *
     * @param viewModel the Builder view model to update
     * @param textFormat the formatter for simulated truth tables
     */
    public BuildCircuitPresenter(
            BuildCircuitViewModel viewModel,
            TruthTableTextFormat textFormat) {
        this(viewModel, textFormat, circuit -> circuit);
    }

    /**
     * Creates a presenter with its preview layout.
     *
     * @param viewModel the Builder view model to update
     * @param textFormat the formatter for simulated truth tables
     * @param previewLayout the layout and routing step for the preview
     */
    public BuildCircuitPresenter(
            BuildCircuitViewModel viewModel,
            TruthTableTextFormat textFormat,
            CircuitPreviewLayout previewLayout) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.textFormat = Objects.requireNonNull(textFormat, "textFormat");
        this.previewLayout = Objects.requireNonNull(previewLayout, "previewLayout");
    }

    @Override
    public void prepareSuccessView(BuildCircuitOutputData outputData) {
        String tableText = textFormat.format(outputData.truthTable());
        Circuit drawing;

        try {
            drawing = previewLayout.layout(outputData.circuit());
        } catch (IllegalArgumentException error) {
            publish(new BuildCircuitState(
                    outputData.circuit().name(),
                    tableText,
                    "Circuit built, but the preview could not be drawn. "
                            + error.getMessage(),
                    outputData.explanation()));
            return;
        }

        List<DrawableComponent> components = drawing.components().stream()
                .map(BuildCircuitPresenter::toDrawableComponent)
                .toList();

        List<DrawableWire> wires = drawing.wires().stream()
                .map(wire -> new DrawableWire(
                        wire.from().x(), wire.from().y(),
                        wire.to().x(), wire.to().y()))
                .toList();

        publish(new BuildCircuitState(
                outputData.circuit().name(),
                tableText,
                "",
                outputData.explanation(),
                components,
                wires));
    }

    @Override
    public void prepareFailView(String errorMessage) {
        publish(new BuildCircuitState("", "", errorMessage));
    }

    private static DrawableComponent toDrawableComponent(CircuitComponent component) {
        return switch (component) {
            case InputPin pin -> drawable(pin, "input", "", pin.label());
            case OutputPin pin -> drawable(pin, "output", "", pin.label());
            case Constant constant -> drawable(
                    constant, "constant", constant.value() ? "1" : "0", "");
            case Gate gate -> drawable(gate, "gate", gate.type().name(), "");
            case UnsupportedComponent unsupported -> drawable(
                    unsupported, "unsupported", unsupported.name(), "");
        };
    }

    private static DrawableComponent drawable(
            CircuitComponent component,
            String kind,
            String detail,
            String label) {
        return new DrawableComponent(
                kind,
                detail,
                label,
                component.location().x(),
                component.location().y(),
                component.facing().name().toLowerCase(Locale.ROOT));
    }

    private void publish(BuildCircuitState state) {
        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }
}
