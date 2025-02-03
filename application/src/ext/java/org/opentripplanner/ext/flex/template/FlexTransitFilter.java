package org.opentripplanner.ext.flex.template;

import java.util.List;

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

  /**
   * Should the trip be considered according to the filter criteria.
   */
  public boolean allowsTrip(ClosestTrip closestTrip) {
    for (var filter : filters) {
      if (!filter.allowsTrip(closestTrip.flexTrip().getTrip())) {
        return false;
      }
    }
    return true;
  }
}
