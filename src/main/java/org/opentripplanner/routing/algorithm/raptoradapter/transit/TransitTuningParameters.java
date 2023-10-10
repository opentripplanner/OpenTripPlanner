package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.site.StopTransferPriority;

public interface TransitTuningParameters {
  List<Duration> PAGING_SEARCH_WINDOW_ADJUSTMENTS = DurationUtils.durations("4h 2h 1h 30m 20m 10m");

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
    @Override
    public boolean enableStopTransferPriority() {
      return true;
    }

    @Override
    public Integer stopTransferCost(StopTransferPriority key) {
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

  /**
   * Return {@code true} to include a cost for each stop for boarding and alighting at the stop
   * given the stop's {@link StopTransferPriority}.
   */
  boolean enableStopTransferPriority();

  /**
   * The stop transfer cost for the given {@link StopTransferPriority}. The cost applied to boarding
   * and alighting all stops with the given priority.
   */
  Integer stopTransferCost(StopTransferPriority key);

  /**
   * The maximum number of transfer RouteRequests for which the pre-calculated transfers should be
   * cached. If too small, the average request may be slower due to the required re-calculating. If
   * too large, more memory may be used than needed.
   */
  int transferCacheMaxSize();

  /**
   * The maximum search window that can be set through the searchWindow API parameter. Due to the
   * way timetable data are collected before a Raptor trip search, using a search window larger than
   * 24 hours may lead to inconsistent search results. Limiting the search window prevents also
   * potential performance issues. The recommended maximum value is 24 hours.
   * This parameter does not restrict the maximum duration of a dynamic search window (use
   * the parameter transit.dynamicSearchWindow.maxWindow to specify such a restriction).
   */
  Duration maxSearchWindow();

  /**
   * This parameter is used to reduce the number of pages a client have to step through for a
   * journey where there are few alternatives/low frequency. This also work well to adjust for
   * periods with infrequent results, like paging through the night. If there are at least 10 trip
   * pr hour during the day and none at night, then this feature will adjust the search-window to
   * around 2 hours during the day ({@code numItineraries=20}) and up to 8h during the night.
   * <p>
   * The provided array of durations is used to increase the search-window for the next/previous
   * page when the current page return few options. If ZERO results is returned the first duration
   * in the list is used, if ONE result is returned then the second duration is used and so on. The
   * duration is added to the existing search-window and inserted into the next and previous page
   * cursor.
   * <p>
   * This parameter controls how the search-window is increased. OTP also reduces the search-window
   * when more than the requested itineraries are fetched. This is done automatically and acn not be
   * configured. Do not be afraid of scaling up fast, it will be reduced to the appropriate level in
   * the next search.
   * <p>
   * The extra time is added to the search-window for the next request if the current result have
   * few itineraries.
   * <p>
   * <p>
   * The default values are: {@link #PAGING_SEARCH_WINDOW_ADJUSTMENTS}
   */
  List<Duration> pagingSearchWindowAdjustments();

  /**
   * {@link RouteRequest}s which will be used at server startup to pre-fill the raptor transfer cache.
   * {@link org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRequestTransferCache}
   */
  List<RouteRequest> transferCacheRequests();
}
