package entity.truthtable;

/**
 * Thrown when an expected truth table's labels do not match the circuit's pins, for example
 * "Your table has inputs {A, B, Cin} but the circuit has {A, B}."
 */
public class LabelMismatchException extends Exception {

    private static final long serialVersionUID = 1L;

    public LabelMismatchException(String message) {
        super(message);
    }
}
