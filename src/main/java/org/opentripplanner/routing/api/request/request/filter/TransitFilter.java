package org.opentripplanner.routing.api.request.request.filter;

import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

public interface TransitFilter {
  /**
   * Return false is route banned, otherwise return true
   */
  boolean matchRoute(Route route);

  /**
   * Return false is tripTimes are banned, otherwise return true
   */
  boolean matchTripTimes(TripTimes trip);

  default boolean isSubModePredicate() {
    return false;
  }

  TransitFilter clone();
}
