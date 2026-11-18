package use_case.build_circuit;

import entity.design.CircuitDesign;

import java.util.Map;
import java.util.Objects;

/**
 * A generated circuit design, its optional expressions, and its explanation.
 *
 * @param design the generated circuit design
 * @param outputExpressions Boolean expressions keyed by output label, or an empty map
 * @param explanation the explanation accompanying the design, or an empty string
 */
public record GeneratedDesign(CircuitDesign design, Map<String, String> outputExpressions,
                              String explanation) {

    /** Validates and copies the generated result. */
    public GeneratedDesign {
        Objects.requireNonNull(design, "design");
        outputExpressions = Map.copyOf(outputExpressions);
        explanation = Objects.requireNonNull(explanation, "explanation").strip();
    }

    /**
     * Creates a generated design without an explanation.
     *
     * @param design the generated circuit design
     * @param outputExpressions Boolean expressions keyed by output label
     */
    public GeneratedDesign(CircuitDesign design, Map<String, String> outputExpressions) {
        this(design, outputExpressions, "");
    }
}
