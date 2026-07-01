package org.opentripplanner.apis.gtfs.model;

import java.util.List;
import java.util.Map;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;

public class CanceledTripsSummaryRoute {

  private final Route route;
  private final Map<TripPattern, Long> cancellationCountsForPatterns;

  public CanceledTripsSummaryRoute(
    Route route,
    Map<TripPattern, Long> cancellationCountsForPatterns
  ) {
    this.route = route;
    this.cancellationCountsForPatterns = cancellationCountsForPatterns;
  }

  public Route route() {
    return this.route;
  }

  public int cancellationCount() {
    return cancellationCountsForPatterns.values().stream().mapToInt(Long::intValue).sum();
  }

  public List<CanceledTripsSummaryPattern> patterns() {
    return cancellationCountsForPatterns
      .entrySet()
      .stream()
      .map(e -> new CanceledTripsSummaryPattern(e.getKey(), e.getValue()))
      .toList();
  }
}
