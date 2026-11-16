package entity.expression;

import java.util.Map;

/** A named variable whose value comes from the evaluation map. */
public record Variable(String name)
        implements BooleanExpression {

    @Override
    public boolean evaluate(Map<String, Boolean> variables) {
        if (!variables.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Unknown variable: " + name);
        }

        return variables.get(name);
    }
}
