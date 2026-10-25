package use_case.import_circuit;

/**
 * A component on the circuit canvas, for the presenter to map into view state.
 *
 * @param kind    one of {@code input}, {@code output}, {@code constant}, {@code gate},
 *                {@code unsupported}
 * @param detail  gate type name (e.g. {@code AND}), constant {@code 0}/{@code 1}, unsupported
 *                component name, or {@code ""} for pins
 * @param label   pin label, or {@code ""} otherwise
 * @param facing  lowercase orientation name (e.g. {@code east})
 */
public record ComponentSummary(String kind, String detail, String label, int x, int y,
                               String facing) {
}
