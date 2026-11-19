package entity.expression;

import java.util.Map;

/** A Boolean expression that combines two operands. */
public record BinaryExpression(BooleanExpression left, Operator operator, BooleanExpression right)
        implements BooleanExpression {

    /** The supported binary Boolean operators. */
    public enum Operator {
        AND,
        XOR,
        OR
    }

    @Override
    public boolean evaluate(Map<String, Boolean> variables) {
        boolean leftValue = left.evaluate(variables);
        boolean rightValue = right.evaluate(variables);

        return switch (operator) {
            case AND -> leftValue && rightValue;
            case XOR -> leftValue ^ rightValue;
            case OR -> leftValue || rightValue;
        };
    }
}
