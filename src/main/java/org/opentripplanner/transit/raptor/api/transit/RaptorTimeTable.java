package org.opentripplanner.transit.raptor.api.transit;

import org.opentripplanner.transit.raptor.api.request.SearchDirection;
import java.util.function.IntUnaryOperator;

/**
 * A TimeTable is a list of trips in service for the given search date and a limited time
 * before and after. This can be a subset of all trips available to speed up the trip search
 * - that is left to the implementation of this interface. Raptor uses a binary search to
 * find the right trip-schedule, so even for long time-tables the Raptor search perform quite
 * well.
 * <p>
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorTimeTable<T extends RaptorTripSchedule> {
    /**
     * Get trip schedule by index. Trip schedules should be listed in order by the
     * departure time for the first stop in the pattern.
     * <p/>
     * This method needs to be FAST - it is in the most critical line of execution in Raptor.
     *
     * @param index the trip schedule index in pattern starting at 0.
     */
    T getTripSchedule(int index);

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

    /**
     * Number of trips in time-table.
     */
    int numberOfTripSchedules();

    /**
     * Raptor provides a trips search for regular trip schedules, but in some cases it makes
     * sense to be able to override this - for example for frequency based trips.
     *
     * @return {@code true} If you do not want to use the built-in trip search and instead
     *         will provide your own. Make sure to implement the
     *         {@link #createCustomizedTripSearch(SearchDirection)} for both forward and reverse
     *         searches.
     */
    boolean useCustomizedTripSearch();

    /**
     * Factory method to provide an alternative trip search in Raptor.
     * @see #useCustomizedTripSearch()
     */
    RaptorTripScheduleSearch<T> createCustomizedTripSearch(SearchDirection direction);
}
