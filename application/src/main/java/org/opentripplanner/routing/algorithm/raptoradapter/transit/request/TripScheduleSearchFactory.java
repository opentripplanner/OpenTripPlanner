package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.raptor.spi.SearchDirection;

/**
 * The purpose of this class is to create a new trip search.
 */
public class TripScheduleSearchFactory {

  /**
   * This threshold is used to determine when to perform a binary trip schedule search
   * to reduce the number of trips departure time lookups and comparisons. When testing
   * with data from Entur and all of Norway as a Graph, the optimal value was about 50.
   * <p/>
   * If you calculate the departure time every time or want to fine tune the performance,
   * changing this may improve the performance a few percent.
   */
  private static final int BINARY_SEARCH_THRESHOLD = 50;

  /**
   * Create a new search based on the given direction:
   * <ou>
   *   <li>FORWARD -> Board search</li>
   *   <li>REVERSE -> Alight search</li>
   * </ou>
   */
  public static <T extends RaptorTripSchedule> RaptorTripScheduleSearch<T> create(
    SearchDirection searchDirection,
    TripSearchTimetable<T> timetable
  ) {
    return searchDirection.isForward()
      ? new TripScheduleBoardSearch<>(timetable, BINARY_SEARCH_THRESHOLD)
      : new TripScheduleAlightSearch<>(timetable, BINARY_SEARCH_THRESHOLD);
  }
}
