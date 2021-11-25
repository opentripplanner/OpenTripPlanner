package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.StopTransferPriority;

public interface TransitTuningParameters {
  /**
   * These tuning parameters are typically used in unit tests. The values are:
   * <pre>
   * enableStopTransferPriority : true
   * stopTransferCost : {
   *   DISCOURAGED:  3600  (equivalent of 1 hour penalty)
   *   ALLOWED:        60  (60 seconds penalty)
   *   RECOMMENDED:    20  (20 seconds penalty)
   *   PREFERRED:       0  (no penalty)
   * }
   * </pre>
   */
  TransitTuningParameters FOR_TEST = new TransitTuningParameters() {
    @Override public boolean enableStopTransferPriority() { return true; }
    @Override public Integer stopTransferCost(StopTransferPriority key) {
      switch (key) {
        case DISCOURAGED: return 3600;
        case ALLOWED:     return 60;
        case RECOMMENDED: return 20;
        case PREFERRED:   return 0;
      }
      throw new IllegalArgumentException("Unknown key: " + key);
    }

    @Override public int transferCacheMaxSize() { return 5; }
  };

  /**
   * Return {@code true} to include a cost for each stop for boarding and alighting at the stop
   * given the stop's {@link StopTransferPriority}.
   */
  boolean enableStopTransferPriority();

  /**
   * The stop transfer cost for the given {@link StopTransferPriority}. The cost applied to
   * boarding and alighting all stops with the given priority.
   */
  Integer stopTransferCost(StopTransferPriority key);

  /**
   * The maximum number of transfer RoutingRequests for which the pre-calculated transfers should be
   * cached. If too small, the average request may be slower due to the required re-calculating. If
   * too large, more memory may be used than needed.
   */
  int transferCacheMaxSize();
}
