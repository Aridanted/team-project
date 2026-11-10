package use_case.import_circuit;

import java.util.List;

/**
 * @param circuitName   name of the opened circuit
 * @param inputLabels   input pin labels in display order
 * @param outputLabels  output pin labels in display order
 * @param components    every component on the canvas
 * @param wires         wire segments for drawing
 * @param warnings      non-fatal problem messages from parsing
 * @param simulatable   whether the circuit can be simulated
 */
public record ImportCircuitOutputData(String circuitName, List<String> inputLabels,
                                      List<String> outputLabels, List<ComponentSummary> components,
                                      List<WireSummary> wires, List<String> warnings,
                                      boolean simulatable) {

    public ImportCircuitOutputData {
        inputLabels = List.copyOf(inputLabels);
        outputLabels = List.copyOf(outputLabels);
        components = List.copyOf(components);
        wires = List.copyOf(wires);
        warnings = List.copyOf(warnings);
    }
}
