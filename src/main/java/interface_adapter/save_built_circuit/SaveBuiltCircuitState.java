package interface_adapter.save_built_circuit;

/**
 * The latest save result displayed by the Builder.
 *
 * @param message the successful save message
 * @param error the saving or verification failure message
 */
public record SaveBuiltCircuitState(String message, String error) {
}
