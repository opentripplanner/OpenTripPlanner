package org.opentripplanner.transit.raptor.api.view;

import java.util.Collection;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.response.StopArrivals;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * The worker perform the travel search. There are multiple implementation, even some who do not
 * return paths.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface Worker<T extends RaptorTripSchedule> {
  /**
   * Perform the routing request.
   *
   */
  void route();

  /**
   * Return all paths found. Am empty set is returned if no paths are found or the algorithm do
   * not collect paths.
   */
  Collection<Path<T>> paths();

  /**
   * Return best over-all-arrival-times, best transit-arrival-times, and lowest number of
   * transfers for all stops.
   */
  StopArrivals stopArrivals();
}
