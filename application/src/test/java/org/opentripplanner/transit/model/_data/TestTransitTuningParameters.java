package org.opentripplanner.transit.model._data;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.site.StopTransferPriority;

class TestTransitTuningParameters implements TransitTuningParameters {

  @Override
  public boolean enableStopTransferPriority() {
    return false;
  }

  @Override
  public Integer stopBoardAlightDuringTransferCost(StopTransferPriority key) {
    return 0;
  }

  @Override
  public int transferCacheMaxSize() {
    return 0;
  }

  @Override
  public Duration maxSearchWindow() {
    return null;
  }

  @Override
  public List<Duration> pagingSearchWindowAdjustments() {
    return List.of();
  }

  @Override
  public List<RouteRequest> transferCacheRequests() {
    return List.of();
  }
}
