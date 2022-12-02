package org.opentripplanner.routing.api.request.request.filter;

import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

public interface FilterPredicate {
  /**
   * Return false is route banned, otherwise return true
   */
  boolean routePredicate(Route route);

  /**
   * Return false is tripTimes are banned, otherwise return true
   */
  boolean tripTimesPredicate(TripTimes trip);

  FilterPredicate clone();
}
