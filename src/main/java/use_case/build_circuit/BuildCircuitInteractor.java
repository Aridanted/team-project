package use_case.build_circuit;

import entity.circuit.Circuit;
import entity.design.CircuitDesign;
import entity.simulation.NotSimulatableException;
import entity.truthtable.TruthTable;
import entity.truthtable.TruthTableGenerator;

/** Builds and verifies a circuit from a student's description. */
public final class BuildCircuitInteractor implements BuildCircuitInputBoundary {
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_ATTEMPTS = 3;

    private final CircuitDesignGenerator generator;
    private final DesignValidator validator;
    private final DesignAssembler assembler;
    private final TruthTableGenerator truthTableGenerator;
    private final OutputExpressionVerifier expressionVerifier;
    private final BuildCircuitOutputBoundary presenter;

    /**
     * Creates the build use case with its generator, verification steps, and presenter.
     *
     * @param generator the source of candidate designs
     * @param validator the circuit design validator
     * @param assembler the converter from design to circuit
     * @param truthTableGenerator the simulator-backed truth table generator
     * @param expressionVerifier the checker for supplied output expressions
     * @param presenter the receiver of the build outcome
     */
    public BuildCircuitInteractor(CircuitDesignGenerator generator, DesignValidator validator,
                                  DesignAssembler assembler, TruthTableGenerator truthTableGenerator,
                                  OutputExpressionVerifier expressionVerifier,
                                  BuildCircuitOutputBoundary presenter) {
        this.generator = generator;
        this.validator = validator;
        this.assembler = assembler;
        this.truthTableGenerator = truthTableGenerator;
        this.expressionVerifier = expressionVerifier;
        this.presenter = presenter;
    }

    @Override
    public void execute(BuildCircuitInputData inputData) {
        String description = inputData.description();
        if (description == null || description.isBlank()) {
            presenter.prepareFailView("Enter a circuit description.");
            return;
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            presenter.prepareFailView("Use at most " + MAX_DESCRIPTION_LENGTH + " characters.");
            return;
        }

        String request = description.strip();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            BuildCircuitOutputData result;
            try {
                result = build(request);
            } catch (DesignGenerationException | NotSimulatableException | IllegalArgumentException error) {
                if (attempt == MAX_ATTEMPTS) {
                    presenter.prepareFailView("Could not build a circuit after " + MAX_ATTEMPTS
                            + " attempts. " + error.getMessage());
                    return;
                }
                continue;
            }
            presenter.prepareSuccessView(result);
            return;
        }
    }

    private BuildCircuitOutputData build(String description)
            throws DesignGenerationException, NotSimulatableException {
        GeneratedDesign generated = generator.generate(description);
        CircuitDesign design = generated.design();
        validator.validate(design);
        Circuit circuit = assembler.assemble(design);
        TruthTable table = truthTableGenerator.generate(circuit);
        expressionVerifier.verify(generated.outputExpressions(), table);
        return new BuildCircuitOutputData(design, circuit, table, generated.explanation());
    }
}
