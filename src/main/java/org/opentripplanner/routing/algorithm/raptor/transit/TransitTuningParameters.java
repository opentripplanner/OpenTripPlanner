package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.TransferPriority;

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
    @Override public Integer stopTransferCost(TransferPriority key) {
      switch (key) {
        case DISCOURAGED: return 3600;
        case ALLOWED:     return 60;
        case RECOMMENDED: return 20;
        case PREFERRED:   return 0;
      }
      throw new IllegalArgumentException("Unknown key: " + key);
    }
  };

  /**
   * Return {@code true} to include a cost for each stop for boarding and alighting at the stop
   * given the stop's {@link TransferPriority}.
   */
  boolean enableStopTransferPriority();

  /**
   * The stop transfer cost for the given {@link TransferPriority}. The cost applied to
   * boarding and alighting all stops with the given priority.
   */
  Integer stopTransferCost(TransferPriority key);
}
