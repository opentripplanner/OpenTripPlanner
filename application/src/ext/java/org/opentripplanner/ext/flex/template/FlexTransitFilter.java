package org.opentripplanner.ext.flex.template;

import java.util.List;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Filters trips based on filter criteria. This is a much reduced version of the general
 * filtering that applies to fixed-schedule transit. It only supports the following features:
 *
 *  - selecting (whitelisting) agencies and routes
 *  - banning (blacklisting) agencies and routes
 */
public class FlexTransitFilter {

  public static FlexTransitFilter ALLOW_ALL = new FlexTransitFilter(
    List.of(new FlexTripFilterRequest.AllowAll())
  );
  private final List<FlexTripFilterRequest> filters;

  public FlexTransitFilter(List<FlexTripFilterRequest> filters) {
    this.filters = filters;
  }

  boolean allowsTrip(ClosestTrip closestTrip) {
    return allowsTrip(closestTrip.flexTrip().getTrip());
  }

  /**
   * Should the trip be considered according to the filter criteria.
   */
  public boolean allowsTrip(Trip trip) {
    for (var filter : filters) {
      if (!filter.allowsTrip(trip)) {
        return false;
      }
    }
    return true;
  }
}
