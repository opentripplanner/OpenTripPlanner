package org.opentripplanner.routing.api.request.framework;

import java.time.Duration;
import org.opentripplanner.framework.model.Cost;

/**
 * A linear function to calculate a new cost based on a cost value input.
 */
public final class CostLinearFunction extends AbstractLinearFunction<Cost> {

  /**
   * Constant is zero and coefficient is zero. The result of {@code f(x) = 0}.
   */
  public static CostLinearFunction ZERO = new CostLinearFunction(Cost.ZERO, 0.0);

  /**
   * Constant is zero and coefficient is one. The result of {@code f(x) = x}.
   */
  public static CostLinearFunction NORMAL = new CostLinearFunction(Cost.ZERO, 1.0);

  private CostLinearFunction(Cost a, double b) {
    super(a, b);
  }

  public static CostLinearFunction of(Cost constant, double coefficient) {
    if (constant.isZero() && coefficient == 0.0) {
      return ZERO;
    }
    return new CostLinearFunction(constant, coefficient);
  }

  public static CostLinearFunction of(Duration constant, double coefficient) {
    return of(Cost.fromDuration(constant), coefficient);
  }

  public static CostLinearFunction of(String text) {
    return LinearFunctionSerialization.parse(text, (Duration a, Double b) ->
      of(Cost.fromDuration(a), b)
    ).orElseThrow();
  }

  public Cost calculate(Cost cost) {
    return constant().plus(cost.multiply(coefficient()));
  }

  @Override
  protected boolean isZero(Cost value) {
    return value.isZero();
  }

  @Override
  protected Duration constantAsDuration() {
    return constant().asDuration();
  }
}
