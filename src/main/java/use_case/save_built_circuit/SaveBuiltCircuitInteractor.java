package use_case.save_built_circuit;

import entity.circuit.Circuit;
import entity.simulation.NotSimulatableException;
import entity.truthtable.TruthTable;
import entity.truthtable.TruthTableGenerator;
import use_case.build_circuit.BuiltCircuitDataAccessInterface;
import use_case.import_circuit.CircuitFileDataAccessInterface;
import use_case.import_circuit.CircuitParseException;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Saves the latest built circuit and verifies it by reloading its truth table. */
public final class SaveBuiltCircuitInteractor implements SaveBuiltCircuitInputBoundary {
    private final BuiltCircuitDataAccessInterface builtCircuits;
    private final CircuitFileWriterInterface writer;
    private final CircuitFileDataAccessInterface reader;
    private final TruthTableGenerator truthTableGenerator;
    private final SaveBuiltCircuitOutputBoundary presenter;

    /**
     * Creates the save use case with its storage and verification dependencies.
     *
     * @param builtCircuits the source of the latest built circuit
     * @param writer the Logisim file writer
     * @param reader the loader used to verify the saved file
     * @param truthTableGenerator the generator for complete truth tables
     * @param presenter the receiver of save outcomes
     */
    public SaveBuiltCircuitInteractor(
            BuiltCircuitDataAccessInterface builtCircuits,
            CircuitFileWriterInterface writer,
            CircuitFileDataAccessInterface reader,
            TruthTableGenerator truthTableGenerator,
            SaveBuiltCircuitOutputBoundary presenter) {
        this.builtCircuits = Objects.requireNonNull(builtCircuits, "builtCircuits");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.reader = Objects.requireNonNull(reader, "reader");
        this.truthTableGenerator = Objects.requireNonNull(
                truthTableGenerator, "truthTableGenerator");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    @Override
    public void execute(String path) {
        if (path == null || path.isBlank()) {
            presenter.prepareFailView("Choose where to save the circuit.");
            return;
        }

        if (!path.toLowerCase(Locale.ROOT).endsWith(".circ")) {
            presenter.prepareFailView("Choose a file name ending in .circ.");
            return;
        }

        Optional<Circuit> stored = builtCircuits.getBuiltCircuit();
        if (stored.isEmpty()) {
            presenter.prepareFailView("Build a circuit before saving.");
            return;
        }

        try {
            Circuit circuit = stored.orElseThrow();
            TruthTable expected = truthTableGenerator.generate(circuit);

            writer.write(circuit, path);
            Circuit reloaded = reader.load(path);

            if (!reloaded.warnings().isEmpty()) {
                presenter.prepareFailView(
                        "The file was written, but reloading reported circuit warnings. "
                                + "The exported circuit has not been verified.");
                return;
            }

            TruthTable actual = truthTableGenerator.generate(reloaded);
            if (!expected.equals(actual)) {
                presenter.prepareFailView(
                        "The file was written, but its truth table differs from "
                                + "the built circuit. The exported circuit has not been verified.");
                return;
            }
        } catch (CircuitFileWriteException | CircuitParseException
                 | NotSimulatableException | IllegalArgumentException error) {
            presenter.prepareFailView(
                    "Could not complete saving and verification. "
                            + error.getMessage());
            return;
        }

        presenter.prepareSuccessView(path);
    }
}
