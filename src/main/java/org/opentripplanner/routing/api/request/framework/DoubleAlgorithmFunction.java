package org.opentripplanner.routing.api.request.framework;

import java.io.Serializable;

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
public interface DoubleAlgorithmFunction extends Serializable {
  /** Perform calculation */
  double calculate(double x);
}
