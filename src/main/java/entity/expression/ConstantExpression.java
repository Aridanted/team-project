package entity.expression;

import java.util.Map;

/** A Boolean literal, represented by 0 or 1 in the input. */
public record ConstantExpression(boolean value) implements BooleanExpression {
    @Override
    public boolean evaluate(Map<String, Boolean> variables) {
        return value;
    }
}
