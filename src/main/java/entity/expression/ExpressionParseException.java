package entity.expression;

/** Indicates that the parser found invalid syntax. */
public class ExpressionParseException extends IllegalArgumentException {
    public ExpressionParseException(String message, int position) {
        super(message + " at position " + position);
    }
}
