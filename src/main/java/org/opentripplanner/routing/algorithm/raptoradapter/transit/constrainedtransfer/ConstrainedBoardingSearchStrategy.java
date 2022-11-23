package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;

/**
 * Used to search forward and in reverse.
 */
interface ConstrainedBoardingSearchStrategy {
  /**
   * <ol>
   * <li>In a forward search return the DEPARTURE time.
   * <li>In a reverse search return the ARRIVAL time.
   * </ol>
   */
  int time(RaptorTripSchedule schedule, int stopPos);

  /**
   * <ol>
   * <li>In a forward search the time is before another time if it is in the PAST.
   * <li>In a reverse search the time is before another time if it is in the FUTURE.
   * </ol>
   */
  boolean timeIsBefore(int time0, int time1);

  /**
   * <ol>
   * <li>In a forward search iterate in departure order.
   * <li>In a reverse search iterate in reverse departure order,
   * starting with the last trip in the schedule.
   * </ol>
   */
  IntIterator scheduleIndexIterator(RaptorTimeTable<TripSchedule> timetable);

  /**
   * <ol>
   * <li>In a forward search add {@code u} and {@code v} together. Example: {@code plus(5, 2) => 7}
   * <li>In a reverse search subtract {@code v} from {@code u}. Example: {@code plus(5, 2) => 3}
   * </ol>
   */
  int plus(int v, int u);

  /**
   * Return the search direction for this strategy.
   */
  SearchDirection direction();

  /**
   * <ol>
   * <li>In a forward search return the highest time.
   * <li>In a reverse search return the lowest time.
   * </ol>
   */
  default int maxTime(int v, int u) {
    return timeIsBefore(v, u) ? u : v;
  }
}
