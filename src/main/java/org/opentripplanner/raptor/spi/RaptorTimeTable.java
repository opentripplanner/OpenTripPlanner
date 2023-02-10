package org.opentripplanner.raptor.spi;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.SearchDirection;

/**
 * A TimeTable is a list of trips in service for the given search date and a limited time before and
 * after. This can be a subset of all trips available to speed up the trip search - that is left to
 * the implementation of this interface. Raptor uses a binary search to find the right
 * trip-schedule, so even for long time-tables the Raptor search perform quite well.
 * <p>
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 * @deprecated TODO RTM - The trip search is modved out of Raptor, so we do not need this class any
 *                        more. Move the tripSeach() method the RaptorRoute - Try to remove the two
 *                        other methods.
 */
@Deprecated
public interface RaptorTimeTable<T extends RaptorTripSchedule> {
  /**
   * Get trip schedule by index. Trip schedules should be listed in order by the departure time for
   * the first stop in the pattern.
   * <p/>
   * This method needs to be FAST - it is in the most critical line of execution in Raptor.
   *
   * @param index the trip schedule index in pattern starting at 0.
   */
  T getTripSchedule(int index);

  /**
   * Number of trips in time-table.
   * @deprecated TODO RTM - We should get rid of this, it is specific to the period we extract data,
   *                        but we get into problems when we want to support unbounded periods. Use
   *                        the trip search instead to find a start point (index) and iterate from
   *                        there.
   */
  @Deprecated
  int numberOfTripSchedules();

  /**
   * Factory method to create the trip search
   */
  RaptorTripScheduleSearch<T> tripSearch(SearchDirection direction);
}
