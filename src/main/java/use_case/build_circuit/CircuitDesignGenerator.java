package use_case.build_circuit;

/** Generates a circuit design from a plain-English description. */
@FunctionalInterface
public interface CircuitDesignGenerator {

    /**
     * Generates a design for the requested circuit.
     *
     * @param description the student's circuit description
     * @return the generated design and any supplied output expressions
     * @throws DesignGenerationException if a design could not be generated
     */
    GeneratedDesign generate(String description) throws DesignGenerationException;
}
