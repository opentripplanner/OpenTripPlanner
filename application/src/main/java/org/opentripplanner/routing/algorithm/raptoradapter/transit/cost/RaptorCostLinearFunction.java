package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.Objects;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.framework.LinearFunctionSerialization;

/**
 * Use primitive types to calculate the cost in Raptor cost-units. This is used to avoid
 * boxing and unboxing as well as casting between different primitive types. This should
 * be fast, but it is not critical.
 * <p>
 * This is used with Raptor, in other places the general-purpose
 * {@link org.opentripplanner.routing.api.request.framework.CostLinearFunction} is preferred.
 * Be aware, the {@link #calculateRaptorCost(int)} slightly differs from
 * {@link org.opentripplanner.routing.api.request.framework.CostLinearFunction#calculate(Cost)}
 * due to rounding.
 */
class RaptorCostLinearFunction {

  private final int constant;

  private final int coefficient;

  static final RaptorCostLinearFunction ZERO_FUNCTION = new RaptorCostLinearFunction(0, 0.0);

  private RaptorCostLinearFunction(int constantSeconds, double coefficient) {
    this.constant = RaptorCostConverter.toRaptorCost(constantSeconds);
    this.coefficient = RaptorCostConverter.toRaptorCost(coefficient);
  }

  static RaptorCostLinearFunction of(int constantSeconds, double coefficient) {
    if (constantSeconds == 0 && coefficient == 0.0) {
      return ZERO_FUNCTION;
    }
    return new RaptorCostLinearFunction(constantSeconds, coefficient);
  }

  static RaptorCostLinearFunction of(CostLinearFunction otpDomainFunction) {
    return of(otpDomainFunction.constant().toSeconds(), otpDomainFunction.coefficient());
  }

  /**
   * Return the extra generalized-cost in Raptor units(centi-seconds)
   */
  int calculateRaptorCost(int durationInSeconds) {
    return constant + coefficient * durationInSeconds;
  }

  boolean isZero() {
    return constant == 0 && coefficient == 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RaptorCostLinearFunction that = (RaptorCostLinearFunction) o;
    return constant == that.constant && coefficient == that.coefficient;
  }

  @Override
  public int hashCode() {
    return Objects.hash(constant, coefficient);
  }

  @Override
  public String toString() {
    return isZero()
      ? "ZERO FUNCTION"
      : LinearFunctionSerialization.serialize(
        RaptorCostConverter.raptorCostToDuration(constant),
        RaptorCostConverter.toOtpDomainFactor(coefficient)
      );
  }
}
