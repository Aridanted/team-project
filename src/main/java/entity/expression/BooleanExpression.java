package entity.expression;

import java.util.Map;

/** A Boolean expression that can be evaluated using variable values. */
public interface BooleanExpression {

    /**
     * Evaluates this expression using the supplied variable values.
     *
     * @param variables values for all variables referenced by this expression
     * @return the result of evaluating this expression
     * @throws IllegalArgumentException if a referenced variable has no mapping
     */
    boolean evaluate(Map<String, Boolean> variables);
}
