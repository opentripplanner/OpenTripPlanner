package org.opentripplanner.routing.api.request.framework;

import java.io.Serializable;
import java.time.Duration;

/**
 * OTP support injecting custom functions into the algorithm. These functions* must support this
 * interface.
 * <p>
 * The implementation must be a value-object. It must implement Serializable, have a nice
 * toString(), like  {@code f(x)= 23 + 1.33 * x}, and implement {@code equals()} and
 * {@code hashCode()} - the function is used as part of a key in a HashMap.
 * <p>
 * Use the {@link org.opentripplanner.routing.api.request.framework.RequestFunctions} to create new
 * objects of this type.
 */
@FunctionalInterface
public interface CostLinearFunction extends Serializable {
  /** Perform calculation */
  int calculate(int timeOrCostInSeconds);

  static CostLinearFunction of(int constantSeconds, double coefficient) {
    return RequestFunctions.createLinearFunction(constantSeconds, coefficient);
  }

  static CostLinearFunction of(Duration constant, double coefficient) {
    return RequestFunctions.createLinearFunction((int) constant.toSeconds(), coefficient);
  }

  static CostLinearFunction of(String text) {
    return RequestFunctions.parse(text);
  }

  default String serialize() {
    return RequestFunctions.serialize(this);
  }
}
