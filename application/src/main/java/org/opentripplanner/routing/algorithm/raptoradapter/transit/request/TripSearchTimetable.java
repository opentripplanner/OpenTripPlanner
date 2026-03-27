package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;

/**
 * This interface adds two methods to the {@link RaptorTimeTable} to optimize the trip search
 * inside the transit model. They were previously in Raptor, but the trip Search is moded outside
 * of Raptor; We have keep these methods in an interface to be able to reuse the complex TripSearch
 * in tests, which do not use the transit model {@link TripSchedule}; Hence also the generic type
 * on this interface.
 */
public interface TripSearchTimetable<T extends RaptorTripSchedule> extends RaptorTimeTable<T> {
  /**
   * Get the arrival time of a specific trip at a specific stop, in seconds from midnight on the
   * search date.
   */
  int arrivalTime(int stopPositionInPattern, int tripIndex);

  /**
   * Get the departure time of a specific trip at a specific stop, in seconds from midnight on the
   * search date.
   */
  int departureTime(int stopPositionInPattern, int tripIndex);
}
