package entity.expression;

/** Indicates that the parser found invalid syntax. */
public class ExpressionParseException extends IllegalArgumentException {
    /**
     * Creates an error that identifies where parsing failed.
     *
     * @param message a description of the syntax error
     * @param position the zero-based character position in the input
     */
    public ExpressionParseException(String message, int position) {
        super(message + " at position " + position);
    }
}
