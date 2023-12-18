package org.opentripplanner.apis.transmodel.model.scalars;

import java.time.Duration;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.framework.TimePenalty;

/**
 * Small DTO class used to map between the OTP Domain model and Transmodel API.
 */
public record DoubleFunction(Duration constant, double coefficient) {
  public static DoubleFunction from(CostLinearFunction v) {
    return new DoubleFunction(v.constant().asDuration(), v.coefficient());
  }

  public static DoubleFunction from(TimePenalty v) {
    return new DoubleFunction(v.constant(), v.coefficient());
  }

  public CostLinearFunction asCostLinearFunction() {
    return CostLinearFunction.of(constant, coefficient);
  }

  public TimePenalty asTimePenalty() {
    return TimePenalty.of(constant, coefficient);
  }
}
