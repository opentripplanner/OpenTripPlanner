package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.site.StopTransferPriority;

public class TransitTuningParametersTestFactory {

  /**
   * These tuning parameters are typically used in unit tests. The values are:
   * <pre>
   * enableStopTransferPriority : true
   * stopBoardAlightDuringTransferCost : {
   *   DISCOURAGED:  3600  (equivalent of 1 hour penalty)
   *   ALLOWED:        60  (60 seconds penalty)
   *   RECOMMENDED:    20  (20 seconds penalty)
   *   PREFERRED:       0  (no penalty)
   * }
   * </pre>
   */
  public static TransitTuningParameters forTest() {
    return new TransitTuningParameters() {
      @Override
      public boolean enableStopTransferPriority() {
        return true;
      }

      @Override
      public Integer stopBoardAlightDuringTransferCost(StopTransferPriority key) {
        switch (key) {
          case DISCOURAGED:
            return 3600;
          case ALLOWED:
            return 60;
          case RECOMMENDED:
            return 20;
          case PREFERRED:
            return 0;
        }
        throw new IllegalArgumentException("Unknown key: " + key);
      }

      @Override
      public int transferCacheMaxSize() {
        return 5;
      }

      @Override
      public Duration maxSearchWindow() {
        return Duration.ofHours(24);
      }

      @Override
      public List<Duration> pagingSearchWindowAdjustments() {
        return PAGING_SEARCH_WINDOW_ADJUSTMENTS;
      }

      @Override
      public List<RouteRequest> transferCacheRequests() {
        return List.of();
      }
    };
  }
}
