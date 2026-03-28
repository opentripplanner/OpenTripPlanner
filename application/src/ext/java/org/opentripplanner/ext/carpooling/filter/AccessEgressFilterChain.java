package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.geometry.WgsCoordinate;

public class AccessEgressFilterChain {

  private final List<AccessEgressTripFilter> filters;

  public AccessEgressFilterChain(List<AccessEgressTripFilter> filters) {
    this.filters = filters;
  }

  /**
   * Creates a standard filter chain with all recommended filters.
   * <p>
   * Filters are ordered by performance impact (fastest first) to maximize
   * the benefit of short-circuit evaluation.
   */
  public static AccessEgressFilterChain standard() {
    return new AccessEgressFilterChain(
      List.of(
        new CapacityFilter(),
        new TimeBasedFilter(),
        new DistanceBasedFilter(),
        new DirectionalCompatibilityFilter()
      )
    );
  }

  /** Runs filters to check whether the carpooling trip is potentially valid.
   *
   * @param trip Carpool trip
   * @param coordinateOfPassenger Coordinate of origin if access, and destination if egress
   * @param passengerDepartureTime Requested departure time of the passenger
   * @param searchWindow The time window around the requested departure time in which the trip will be considered
   * @return true if all the filters return true, false if one of them returns false
   */
  public boolean accepts(
    CarpoolTrip trip,
    WgsCoordinate coordinateOfPassenger,
    Instant passengerDepartureTime,
    Duration searchWindow
  ) {
    return filters
      .stream()
      .allMatch(filter ->
        filter.acceptsAccessEgress(
          trip,
          coordinateOfPassenger,
          passengerDepartureTime,
          searchWindow
        )
      );
  }
}
