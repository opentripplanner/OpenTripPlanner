package org.opentripplanner.transit.raptor.api.transit;

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
     * value takes the index the trip schedule index in pattern starting at 0 as input and returns
     * the arrival time.
     */
    IntUnaryOperator getArrivalTimes(int stopPositionInPattern);

    /**
     * Get the departure times of all trips at a specific stop index, sorted by time. The returned
     * value takes the index the trip schedule index in pattern starting at 0 as input and returns
     * the departure time.
     */
    IntUnaryOperator getDepartureTimes(int stopPositionInPattern);

    /**
     * Number of trips in time-table.
     */
    int numberOfTripSchedules();
}
