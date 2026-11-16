package use_case.build_circuit;

import entity.design.CircuitDesign;

import java.util.Map;
import java.util.Objects;

/**
 * A generated circuit design and its optional Boolean expressions.
 *
 * @param design the generated circuit design
 * @param outputExpressions Boolean expressions keyed by output label, or an empty map
 */
public record GeneratedDesign(CircuitDesign design, Map<String, String> outputExpressions) {
    public GeneratedDesign {
        Objects.requireNonNull(design, "design");
        outputExpressions = Map.copyOf(outputExpressions);
    }
}
