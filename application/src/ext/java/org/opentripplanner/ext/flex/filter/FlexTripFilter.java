package org.opentripplanner.ext.flex.filter;

import java.util.List;
import org.opentripplanner.ext.flex.filter.FlexTripFilterRequest.AllowAll;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Filters trips based on filter criteria. This is a much reduced version of the general
 * filtering that applies to fixed-schedule transit. It only supports the following features:
 *
 *  - selecting (whitelisting) agencies and routes
 *  - banning (blacklisting) agencies and routes
 */
public class FlexTripFilter {

  public static FlexTripFilter ALLOW_ALL = new FlexTripFilter(List.of(AllowAll.of()));
  private final List<FlexTripFilterRequest> filters;

  public FlexTripFilter(List<FlexTripFilterRequest> filters) {
    this.filters = filters;
  }

  /**
   * Should the trip be used in the routing according to the filter criteria.
   */
  public boolean allowsTrip(Trip trip) {
    for (var filter : filters) {
      if (!filter.allowsTrip(trip)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof FlexTripFilter filter) {
      return this.filters.equals(filter.filters);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(FlexTripFilter.class).addCol("filters", filters).toString();
  }
}
