package org.opentripplanner.ext.transmodelapi.model.scalars;

import java.time.Duration;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.framework.TimePenalty;

/**
 * Small DTO class used to map between the OTP Domain model and Transmodel API.
 */
public record LinearFunction(Duration constant, double coefficient) {
  public static LinearFunction from(CostLinearFunction v) {
    return new LinearFunction(v.constant().asDuration(), v.coefficient());
  }

  public static LinearFunction from(TimePenalty v) {
    return new LinearFunction(v.constant(), v.coefficient());
  }

  public CostLinearFunction asCostLinearFunction() {
    return CostLinearFunction.of(constant, coefficient);
  }

  public TimePenalty asTimePenalty() {
    return TimePenalty.of(constant, coefficient);
  }
}
