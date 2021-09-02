package org.opentripplanner.transit.raptor.api.transit;


/**
 * The purpose of the TripScheduleBoardAlight is to represent the board/alight for a
 * given trip at a specific stop. This is used as a result for the trip search, but may also
 * be used in other situation where a search is unnecessary like a guaranteed transfer.
 * <p>
 * An instance of this class is passed on to the algorithm to perform the boarding and contain the
 * necessary information to do so.
 * <p>
 * The instance can represent both the result of a forward search and the result of a reverse
 * search. For a reverse search (searching backward in time) the trip arrival times should be
 * used. This is one of the things that allows for the algorithm to be generic, used
 * in both cases.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorTripScheduleBoardOrAlightEvent<T extends RaptorTripSchedule> {

    /**
     * The trip timetable index for the trip  found.
     */
    int getTripIndex();

    /**
     * This i a reference to the trip found.
     */
    T getTrip();

    /**
     * Return the stop-position-in-pattern for the current trip board search.
     */
    int getStopPositionInPattern();

    /**
     * Get the board/alight time for the trip found.
     * In the case of a normal search the boarding time should be returned,
     * and in the case of a reverse search the alight time should be returned.
     */
    int getTime();
}
