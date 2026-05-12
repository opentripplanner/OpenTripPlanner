package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;

/**
 * Combines multiple {@link CarpoolTripFilter}s using AND logic (all filters must pass).
 * <p>
 * Applied before routing to reduce computational cost by eliminating trip candidates that are
 * incompatible based on estimated times and geographic proximity. Filters are evaluated in order,
 * with short-circuit evaluation: as soon as one filter rejects a trip, evaluation stops.
 * <p>
 * The standard configuration includes (in order of performance impact):
 * <ol>
 *   <li>{@link DepartAfterTripFilter} / {@link ArriveByTripFilter} — O(1) time checks</li>
 *   <li>{@link DistanceBasedFilter} — O(1) with a few distance calculations</li>
 * </ol>
 */
public class TripPreFilters implements CarpoolTripFilter {

  private final List<CarpoolTripFilter> filters;

  public TripPreFilters(List<CarpoolTripFilter> filters) {
    this.filters = filters;
  }

  /** Creates a standard pre-filter with all recommended filters in performance order. */
  public static TripPreFilters standard() {
    return new TripPreFilters(
      List.of(new DepartAfterTripFilter(), new ArriveByTripFilter(), new DistanceBasedFilter())
    );
  }

  @Override
  public boolean isCandidateTrip(
    CarpoolTrip trip,
    CarpoolingRequest request,
    Duration searchWindow
  ) {
    return filters.stream().allMatch(filter -> filter.isCandidateTrip(trip, request, searchWindow));
  }
}
