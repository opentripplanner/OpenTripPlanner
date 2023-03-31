package org.opentripplanner.routing.algorithm.transferoptimization.api;

import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.raptor.api.path.RaptorPath;

/**
 * @see org.opentripplanner.routing.algorithm.transferoptimization package documantation.
 */
public interface TransferOptimizationParameters {
  /**
   * If enabled, all paths will be optimized with respect to the transfer point to minimise the
   * {@link org.opentripplanner.model.transfer.TransferConstraint#cost(TransferConstraint)}.
   */
  boolean optimizeTransferPriority();

  /**
   * This enables the transfer wait time optimization. If not enabled the {@link
   * RaptorPath#c1()} function is used to pick the optimal transfer point.
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

  /**
   * Use this to add an extra board- and alight-cost for (none) prioritized stops. A {@code
   * stopBoardAlightCosts} is added to the generalized-cost during routing. But, this cost cannot be
   * too high, because that would add extra cost to the transfer, and favor other alternative paths.
   * But, when optimizing transfers, we do not have to take other paths into consideration and can
   * "boost" the stop-priority-cost to allow transfers to take place at a preferred stop.
   * <p>
   * The cost added during routing is already added to the generalized-cost used as a base in the
   * optimized transfer calculation. By setting this parameter to 0, no extra cost is added, by
   * setting it to {@code 1.0} the stop-cost is doubled.
   * <p>
   * Stop priority is only supported by the NeTEx import, not GTFS.
   * <p>
   * Default value is 0.0.
   */
  double extraStopBoardAlightCostsFactor();
}
