package interface_adapter.import_circuit;

/**
 * A component on the circuit canvas, for views to draw without depending on use-case types.
 *
 * @param kind    one of {@code input}, {@code output}, {@code constant}, {@code gate},
 *                {@code unsupported}
 * @param detail  gate type, constant value, unsupported name, or {@code ""} for pins
 * @param label   pin label, or {@code ""} otherwise
 * @param facing  lowercase orientation name
 */
public record DrawableComponent(String kind, String detail, String label, int x, int y,
                                String facing) {
}
