package org.opentripplanner.raptor.api.request;

/**
 * Tuning parameters - changing these parameters change the performance (speed and/or memory
 * consumption).
 */
public interface RaptorTuningParameters {
  /** see {@link org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig} **/
  default int maxNumberOfTransfers() {
    return 12;
  }

  /** see {@link org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig} **/
  default int scheduledTripBinarySearchThreshold() {
    return 50;
  }

  /** see {@link org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig} **/
  default int iterationDepartureStepInSeconds() {
    return 60;
  }

  /**
   * Coefficients used to calculate raptor-search-window parameters dynamically from heuristics.
   */
  default DynamicSearchWindowCoefficients dynamicSearchWindowCoefficients() {
    return new DynamicSearchWindowCoefficients() {};
  }

  /**
   * Enable the Transfer Early Pruning optimization for standard RAPTOR.
   * <p>
   * When enabled, the transfer loop breaks early once the arrival time exceeds the best known
   * destination arrival time. This requires transfers to be sorted by duration. The optimization
   * is only applied in Standard RAPTOR — see {@link
   * org.opentripplanner.raptor.rangeraptor.standard.StdTransferEarlyPruning} for details.
   *
   * @see org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig
   */
  default boolean earlyTransferPruning() {
    return true;
  }
}
