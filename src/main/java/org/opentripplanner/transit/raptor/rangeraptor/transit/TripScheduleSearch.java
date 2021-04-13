package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * The purpose of the TripScheduleSearch is to optimize the search for a trip schedule
 * for a given pattern. Normally the search scan trips sequentially, aborting when a
 * valid trip is found, it can do so because the trips are ordered after the FIRST stop
 * alight/board times. We also assume that trips do not pass each other; Hence trips in
 * service on a given day will be in order for all other stops too.
 * <p/>
 * The search use a binary search if the number of trip schedules is above a given
 * threshold. A linear search is slow when the number of schedules is very large, let
 * say more than 300 trip schedules.
 * <p/>
 * The implementation of this interface take care the search direction (forward/reverse).
 * For a reverse search (searching backward in time) the trip found departure/arrival times
 * are swapped. This is one of the things that allows for the algorithm to be generic, used
 * in both cases.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface TripScheduleSearch<T extends RaptorTripSchedule> {
    /** Used in a trip search to indicate that all trips should be included in the search. */
    int UNBOUNDED_TRIP_INDEX = -1;

    /**
     * Find the best trip matching the given {@code timeLimit}.
     * This is the same as calling {@link #search(int, int, int)} with {@code tripIndexLimit: -1}.
     *
     * @see #search(int, int, int)
     */
    default boolean search(int timeLimit, int stopPositionInPattern) {
        return search(timeLimit, stopPositionInPattern, UNBOUNDED_TRIP_INDEX);
    };

    /**
     * Find the best trip matching the given {@code timeLimit} and {@code tripIndexLimit}.
     *
     * @param arrivalDepartureTime  The time of arrival(departure for reverse search) at the given stop.
     * @param stopPositionInPattern The stop to board
     * @param tripIndexLimit        Upper bound for trip index to search. Inclusive. Use
     *                              {@code -1} for an unbounded search. This is an optimization
     *                              which allow us to search faster, and it exclude results witch
     *                              is less favorable than trips already processed.
     */
    boolean search(int arrivalDepartureTime, int stopPositionInPattern, int tripIndexLimit);

    /**
     * Return the earliest-board-time for the current trip board search.
     * Latest-arrival-time for alight search.
     */
    int getEarliestBoardTime();

    /**
     * Return the stop-position-in-pattern for the current trip board search.
     */
    int getStopPositionInPattern();

    /**
     * This i a reference to the trip found.
     */
    T getCandidateTrip();

    /**
     * The trip index of the last trip found.
     */
    int getCandidateTripIndex();

    /**
     * Get the board/alight time for the trip found.
     * In the case of a normal search the boarding time should be returned,
     * and in the case of a reverse search the alight time should be returned.
     */
    int getCandidateTripTime();
}
