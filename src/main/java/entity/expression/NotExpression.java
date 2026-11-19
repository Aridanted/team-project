package entity.expression;

import java.util.Map;

/** A Boolean expression that negates its operand. */
public record NotExpression(BooleanExpression operand) implements BooleanExpression {
    @Override
    public boolean evaluate(Map<String, Boolean> variables) {
        return !operand.evaluate(variables);
    }
}
