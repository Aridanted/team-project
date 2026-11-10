/**
 * Frameworks and drivers on the data side: the .circ parser, LLM clients, JSON stores and the
 * in-memory session store. Classes here implement gateway interfaces declared in
 * {@code use_case}.
 *
 * <p>May depend on {@code use_case} and {@code entity}; never on {@code interface_adapter},
 * {@code view} or {@code app}. Enforced by {@code architecture.DependencyRuleTest}.
 */
package data_access;
