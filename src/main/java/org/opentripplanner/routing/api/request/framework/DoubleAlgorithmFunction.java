package org.opentripplanner.routing.api.request.framework;

import java.io.Serializable;

/**
 * OTP support injecting custom functions into the algorithm. These functions
 * must support this interface.
 */
@FunctionalInterface
public interface DoubleAlgorithmFunction extends Serializable {
  /** Perform calculation */
  double calculate(double x);
}
