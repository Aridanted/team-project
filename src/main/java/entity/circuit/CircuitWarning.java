package entity.circuit;

import java.util.Objects;

/**
 * A non-fatal problem found while reading a circuit.
 *
 * @param code    stable identifier such as "W101", for tests and documentation
 * @param message human-readable explanation
 */
public record CircuitWarning(String code, String message) {

    public CircuitWarning {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
