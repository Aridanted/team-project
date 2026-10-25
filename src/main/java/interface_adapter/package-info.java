/**
 * Interface adapters: controllers (call input boundaries), presenters (implement output
 * boundaries) and view models / states (observed by views).
 *
 * <p>May depend on {@code use_case} and {@code entity}; never on {@code view},
 * {@code data_access}, {@code app} or Swing. Enforced by {@code architecture.DependencyRuleTest}.
 */
package interface_adapter;
