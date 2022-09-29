package org.opentripplanner.routing.core;

import java.time.Instant;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;

public class AStarRequest {

  // the time at which the search started
  public final Instant startTime;

  protected final RouteRequest opt;

  /**
   * The requested mode for the search. This contains information about all allowed transitions
   * between the different traverse modes, such as renting or parking a vehicle. Contrary to
   * currentMode, which can change when traversing edges, this is constant for a single search.
   */
  protected final StreetMode requestMode;

  public AStarRequest(Instant startTime, RouteRequest opt, StreetMode requestMode) {
    this.startTime = startTime;
    this.opt = opt;
    this.requestMode = requestMode;
  }
}
