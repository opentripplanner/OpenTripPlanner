package org.opentripplanner.routing.algorithm.transferoptimization.api;


import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.transit.raptor.api.path.Path;

/**
 * @see org.opentripplanner.routing.algorithm.transferoptimization package documantation.
 */
public interface TransferOptimizationParameters {

  /**
   * If enabled, all paths will be optimized with respect to the transfer point to minimise
   * the {@link org.opentripplanner.model.transfer.TransferConstraint#cost(TransferConstraint)}.
   */
  boolean optimizeTransferPriority();

  /**
   * This enables the transfer wait time optimization. If not enabled the {@link
   * Path#generalizedCost()} function is used to pick the optimal transfer point.
   */
  boolean optimizeTransferWaitTime();

  /**
   * The wait time is used to prevent "back-travel", the {@code backTravelWaitTimeFactor} is
   * multiplied with the {@code wait-time} and subtracted from the optimized-transfer-cost.
   */
  double backTravelWaitTimeFactor();

  /**
   * This defines the maximum cost for the logarithmic function relative to the {@code
   * min-safe-transfer-time (t0)} when wait time goes towards zero(0).
   * <pre>
   * f(0) = n * t0
   * </pre>
   */
  double minSafeWaitTimeFactor();
}
