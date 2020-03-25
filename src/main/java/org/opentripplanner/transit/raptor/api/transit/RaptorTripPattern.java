package org.opentripplanner.transit.raptor.api.transit;


/**
 * This interface represent a trip pattern.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorTripPattern<T extends RaptorTripSchedule> {

    /**
     * The stop index
     * @param stopPositionInPattern stop position number in pattern, starting at 0.
     */
    int stopIndex(int stopPositionInPattern);

    /**
     * Number of stops in pattern.
     */
    int numberOfStopsInPattern();

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
     * Number of trips in pattern.
     */
    int numberOfTripSchedules();
}
