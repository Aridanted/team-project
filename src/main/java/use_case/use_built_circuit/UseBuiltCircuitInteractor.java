package use_case.use_built_circuit;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.CircuitWarning;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.InputPin;
import entity.circuit.OutputPin;
import entity.circuit.UnsupportedComponent;
import use_case.build_circuit.BuiltCircuitDataAccessInterface;
import use_case.build_circuit.CircuitPreviewLayout;
import use_case.import_circuit.ComponentSummary;
import use_case.import_circuit.ImportCircuitDataAccessInterface;
import use_case.import_circuit.ImportCircuitOutputBoundary;
import use_case.import_circuit.ImportCircuitOutputData;
import use_case.import_circuit.WireSummary;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Makes the latest built circuit available in Workspace. */
public final class UseBuiltCircuitInteractor implements UseBuiltCircuitInputBoundary {
    private final BuiltCircuitDataAccessInterface builtCircuits;
    private final CircuitPreviewLayout layout;
    private final ImportCircuitDataAccessInterface session;
    private final ImportCircuitOutputBoundary presenter;

    /**
     * Creates the use case with its circuit store and Workspace dependencies.
     *
     * @param builtCircuits the source of the latest built circuit
     * @param layout the layout and routing step
     * @param session the store for the current Workspace circuit
     * @param presenter the presenter that updates the Workspace circuit display
     */
    public UseBuiltCircuitInteractor(
            BuiltCircuitDataAccessInterface builtCircuits,
            CircuitPreviewLayout layout,
            ImportCircuitDataAccessInterface session,
            ImportCircuitOutputBoundary presenter) {
        this.builtCircuits = Objects.requireNonNull(builtCircuits, "builtCircuits");
        this.layout = Objects.requireNonNull(layout, "layout");
        this.session = Objects.requireNonNull(session, "session");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    @Override
    public void execute() {
        Optional<Circuit> stored = builtCircuits.getBuiltCircuit();
        if (stored.isEmpty()) {
            presenter.prepareFailView("Build a circuit before using it in Workspace.");
            return;
        }

        Circuit circuit;
        ImportCircuitOutputData output;

        try {
            circuit = layout.layout(stored.orElseThrow());
            output = outputData(circuit);
        } catch (IllegalArgumentException error) {
            presenter.prepareFailView(
                    "Could not prepare the built circuit for Workspace. "
                            + error.getMessage());
            return;
        }

        session.setCurrentCircuit(circuit);
        presenter.prepareSuccessView(output);
    }

    private static ImportCircuitOutputData outputData(Circuit circuit) {
        return new ImportCircuitOutputData(
                circuit.name(),
                circuit.inputPins().stream().map(InputPin::label).toList(),
                circuit.outputPins().stream().map(OutputPin::label).toList(),
                circuit.components().stream()
                        .map(UseBuiltCircuitInteractor::componentSummary)
                        .toList(),
                circuit.wires().stream()
                        .map(wire -> new WireSummary(
                                wire.from().x(), wire.from().y(),
                                wire.to().x(), wire.to().y()))
                        .toList(),
                circuit.warnings().stream().map(CircuitWarning::message).toList(),
                circuit.isSimulatable());
    }

    private static ComponentSummary componentSummary(CircuitComponent component) {
        return switch (component) {
            case InputPin pin -> summary(pin, "input", "", pin.label());
            case OutputPin pin -> summary(pin, "output", "", pin.label());
            case Constant constant -> summary(
                    constant, "constant", constant.value() ? "1" : "0", "");
            case Gate gate -> summary(gate, "gate", gate.type().name(), "");
            case UnsupportedComponent unsupported -> summary(
                    unsupported, "unsupported", unsupported.name(), "");
        };
    }

    private static ComponentSummary summary(
            CircuitComponent component,
            String kind,
            String detail,
            String label) {
        return new ComponentSummary(
                kind,
                detail,
                label,
                component.location().x(),
                component.location().y(),
                component.facing().name().toLowerCase(Locale.ROOT));
    }
}
