package org.opentripplanner.apis.gtfs.model;

import java.util.List;
import java.util.Map;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;

public class CanceledTripsSummary {

  private final Map<Route, Map<TripPattern, Long>> cancellationSummaryForRoutes;

  public CanceledTripsSummary(Map<Route, Map<TripPattern, Long>> cancellationSummaryForRoutes) {
    this.cancellationSummaryForRoutes = cancellationSummaryForRoutes;
  }

  public List<CanceledTripsSummaryRoute> routes() {
    return cancellationSummaryForRoutes
      .entrySet()
      .stream()
      .map(entry -> new CanceledTripsSummaryRoute(entry.getKey(), entry.getValue()))
      .toList();
  }
}
