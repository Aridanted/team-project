package entity.expression;

import java.util.Map;

/** A Boolean expression that can be evaluated using variable values. */
public interface BooleanExpression {

    boolean evaluate(Map<String, Boolean> variables);
}
