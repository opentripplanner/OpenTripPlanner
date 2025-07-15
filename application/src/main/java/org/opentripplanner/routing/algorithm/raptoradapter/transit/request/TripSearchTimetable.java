package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.function.IntUnaryOperator;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
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
   * Get the arrival times of all trips at a specific stop index, sorted by time. The returned
   * function takes the trip index in the TimeTable as input and returns the arrival time as
   * seconds from midnight on the search date.
   */
  IntUnaryOperator getArrivalTimes(int stopPositionInPattern);

  /**
   * Get the departure times of all trips at a specific stop index, sorted by time. The returned
   * function takes the trip index in the TimeTable as input and returns the departure time as
   * seconds from midnight on the search date.
   */
  IntUnaryOperator getDepartureTimes(int stopPositionInPattern);
}
