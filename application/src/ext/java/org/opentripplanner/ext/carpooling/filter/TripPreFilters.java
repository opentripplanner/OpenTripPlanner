package org.opentripplanner.ext.carpooling.filter;

import java.util.List;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;

/**
 * Combines multiple {@link CarpoolTripFilter}s using AND logic (all filters must pass).
 * <p>
 * Applied before routing to reduce computational cost by eliminating trip candidates that cannot
 * serve the snapped passenger in time or in space. Filters are evaluated in order,
 * with short-circuit evaluation: as soon as one filter rejects a trip, evaluation stops.
 * <p>
 * The standard configuration includes (in order of performance impact):
 * <ol>
 *   <li>{@link TimeTripFilter} — O(1) time checks for both depart-after and arrive-by</li>
 *   <li>{@link CorridorTripFilter} — a beeline check per leg of the trip's corridor</li>
 * </ol>
 */
public class TripPreFilters implements CarpoolTripFilter {

  private final List<CarpoolTripFilter> filters;

  public TripPreFilters(List<CarpoolTripFilter> filters) {
    this.filters = filters;
  }

  /**
   * Creates a default pre-filter with all recommended filters in performance order.
   *
   * @param maxCarSpeed the fastest car speed in the street graph, in metres per second
   */
  public static TripPreFilters defaults(double maxCarSpeed) {
    return new TripPreFilters(List.of(new TimeTripFilter(), new CorridorTripFilter(maxCarSpeed)));
  }

  @Override
  public boolean isCandidateTrip(RoutableCarpoolTrip trip, SnappedPassenger passenger) {
    return filters.stream().allMatch(filter -> filter.isCandidateTrip(trip, passenger));
  }
}
