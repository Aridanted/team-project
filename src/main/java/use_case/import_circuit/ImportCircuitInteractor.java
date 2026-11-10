package use_case.import_circuit;

import entity.circuit.Circuit;
import entity.circuit.CircuitComponent;
import entity.circuit.CircuitWarning;
import entity.circuit.Constant;
import entity.circuit.Gate;
import entity.circuit.InputPin;
import entity.circuit.Orientation;
import entity.circuit.OutputPin;
import entity.circuit.UnsupportedComponent;
import entity.circuit.Wire;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Opens a .circ file, stores it in the session, and prepares data for the view. */
public final class ImportCircuitInteractor implements ImportCircuitInputBoundary {

    private final CircuitFileDataAccessInterface circuitFiles;
    private final ImportCircuitDataAccessInterface session;
    private final ImportCircuitOutputBoundary presenter;

    /**
     * @param circuitFiles gateway that reads circuit files
     * @param session      stores the current circuit after a successful open
     * @param presenter    output port for success and failure views
     */
    public ImportCircuitInteractor(CircuitFileDataAccessInterface circuitFiles,
                                   ImportCircuitDataAccessInterface session,
                                   ImportCircuitOutputBoundary presenter) {
        this.circuitFiles = Objects.requireNonNull(circuitFiles, "circuitFiles");
        this.session = Objects.requireNonNull(session, "session");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    @Override
    public void execute(ImportCircuitInputData inputData) {
        Objects.requireNonNull(inputData, "inputData");
        String path = inputData.filePath();
        if (path == null || path.isBlank()) {
            presenter.prepareFailView("Choose a .circ file to open.");
            return;
        }
        if (!path.toLowerCase(Locale.ROOT).endsWith(".circ")) {
            presenter.prepareFailView("Only Logisim .circ files can be opened.");
            return;
        }
        final Circuit circuit;
        try {
            circuit = circuitFiles.load(path);
        } catch (CircuitParseException e) {
            presenter.prepareFailView(e.getMessage());
            return;
        }
        session.setCurrentCircuit(circuit);
        presenter.prepareSuccessView(buildOutputData(circuit));
    }

    private static ImportCircuitOutputData buildOutputData(Circuit circuit) {
        List<String> inputLabels = circuit.inputPins().stream().map(InputPin::label).toList();
        List<String> outputLabels = circuit.outputPins().stream().map(OutputPin::label).toList();
        List<ComponentSummary> components = circuit.components().stream()
                .map(ImportCircuitInteractor::toComponentSummary)
                .toList();
        List<WireSummary> wires = circuit.wires().stream()
                .map(ImportCircuitInteractor::toWireSummary)
                .toList();
        List<String> warnings = circuit.warnings().stream()
                .map(CircuitWarning::message)
                .toList();
        return new ImportCircuitOutputData(
                circuit.name(),
                inputLabels,
                outputLabels,
                components,
                wires,
                warnings,
                circuit.isSimulatable());
    }

    private static ComponentSummary toComponentSummary(CircuitComponent component) {
        return switch (component) {
            case InputPin pin -> new ComponentSummary(
                    "input",
                    "",
                    pin.label(),
                    pin.location().x(),
                    pin.location().y(),
                    facingName(pin.facing()));
            case OutputPin pin -> new ComponentSummary(
                    "output",
                    "",
                    pin.label(),
                    pin.location().x(),
                    pin.location().y(),
                    facingName(pin.facing()));
            case Constant constant -> new ComponentSummary(
                    "constant",
                    constant.value() ? "1" : "0",
                    "",
                    constant.location().x(),
                    constant.location().y(),
                    facingName(constant.facing()));
            case Gate gate -> new ComponentSummary(
                    "gate",
                    gate.type().name(),
                    "",
                    gate.location().x(),
                    gate.location().y(),
                    facingName(gate.facing()));
            case UnsupportedComponent unsupported -> new ComponentSummary(
                    "unsupported",
                    unsupported.name(),
                    "",
                    unsupported.location().x(),
                    unsupported.location().y(),
                    facingName(unsupported.facing()));
        };
    }

    private static WireSummary toWireSummary(Wire wire) {
        return new WireSummary(
                wire.from().x(),
                wire.from().y(),
                wire.to().x(),
                wire.to().y());
    }

    private static String facingName(Orientation facing) {
        return facing.name().toLowerCase(Locale.ROOT);
    }
}
