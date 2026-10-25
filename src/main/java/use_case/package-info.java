/**
 * Application business rules. One sub-package per use case, each containing its input
 * boundary, input data, interactor, output boundary, output data and the data access
 * interfaces (gateways) that the interactor owns.
 *
 * <p>May depend only on {@code entity}. Outer layers implement the interfaces declared here
 * (dependency inversion). Enforced by {@code architecture.DependencyRuleTest}.
 */
package use_case;
