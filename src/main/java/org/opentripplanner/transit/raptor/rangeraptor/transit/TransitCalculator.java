package org.opentripplanner.transit.raptor.rangeraptor.transit;


import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import static org.opentripplanner.transit.raptor.util.TimeUtils.hm2time;

/**
 * The transit calculator is used to calculate transit related stuff, like calculating
 * <em>earliest boarding time</em> and time-shifting the access legs.
 * <p/>
 * The calculator is shared between the state, worker and path mapping code. This
 * make the calculations consistent and let us hide the request parameters. Hiding the
 * request parameters ensure that this calculator is used.
 */
public interface TransitCalculator {

    /**
     * Use this constant to represent an uninitialized time value.
     */
    int TIME_NOT_SET = SearchParams.TIME_NOT_SET;

    /**
     * Add duration to time and return the result. In the case of a normal
     * forward search this will be a plus '+' operation, while in a reverse
     * search (moving back in time) this will be a minus '-' operation: 'time - duration'.
     */
    int plusDuration(int time, int duration);

    /**
     * Subtract a positive duration from given time and return the result. In the
     * case of a normal forward search this will be a minus '-' operation, while in
     * a reverse search (moving back in time) this will be a plus '+' operation.
     */
    int minusDuration(int time, int duration);

    /**
     * Subtract a time (B) from time (A) and return the result. In the case of
     * a normal forward search this will be: 'B - A' operation, while in
     * a reverse search (moving back in time) this will 'A - B'.
     */
    int duration(int timeA, int timeB);

    /**
     * For a normal search return the trip arrival time at stop position.
     * For a reverse search return the next trips departure time at stop position with the boardSlack added.
     *
     * @param onTrip the current boarded trip
     * @param stopPositionInPattern the stop position/index
     * @param <T> The TripSchedule type defined by the user of the raptor API.
     */
    <T extends RaptorTripSchedule> int stopArrivalTime(T onTrip, int stopPositionInPattern, int alightSlack);


    /**
     * Stop the search when the time exceeds the latest-acceptable-arrival-time.
     * In a reverse search this is the earliest acceptable departure time.
     *
     * @return true if time exceeds limit, false means good to go.
     */
    boolean exceedsTimeLimit(int time);

    /**
     * Return a reason why a arrival time do not pass the {@link #exceedsTimeLimit(int)}
     */
    String exceedsTimeLimitReason();

    /**
     * Return true is the first argument (subject) is the best time, and false if not. If both
     * are equal false is retuned.
     * <p/>
     * In a normal forward search "best" is considered BEFORE in time, while AFTER in time
     * is considered best in a reverse seach.
     *
     * @return true is subject is better then the candidate; if not false.
     */
    boolean isBest(int subject, int candidate);

    /**
     * Uninitialized time values is set to this value to mark them as not set, and to mark the
     * arrival as unreached. A big value(or very small value) is used to simplify the comparisons
     * to see if a new arrival time is better (less).
     * <p/>
     * For a normal forward search this should be Integer.MAX_VALUE and for a reverse
     * search this should be Integer.MIN_VALUE.
     */
    int unreachedTime();

    /**
     * Selects the earliest or latest possible departure time depending on the direction.
     * For forward search it will be the earliest possible departure time, while for reverse search
     * it uses the latest arrival time.
     */
    int departureTime(RaptorTransfer transfer, int departureTime);

    /**
     * Return an iterator, iterating over the minutes in the RangeRaptor algorithm.
     */
    IntIterator rangeRaptorMinutes();

    /**
     * Return TRUE if the Range Raptor should perform only ONE iteration.
     * This is defined happens if the search window is less than or equals
     * to the iteration step duration.
     */
    boolean oneIterationOnly();

    /**
     * Return an iterator, iterating over the stop positions in a pattern.
     * Iterate from '0' to 'nStopsInPattern - 1' in a forward search and from
     * 'nStopsInPattern - 1' to '0' in a reverse search.
     *
     * @param nStopsInPattern the number of stops in the trip pattern
     */
    IntIterator patternStopIterator(int nStopsInPattern);

    /**
     * Return an iterator, iterating over the stop positions in a pattern.
     * Iterate from 'onTripStopPos + 1' to 'nStopsInPattern-1' in a forward search
     * and from 'onTripStopPos - 1' to 0 in a reverse search.
     *
     * @param onTripStopPos the iterator will start here(exclusive)
     * @param nStopsInPattern the number of stops in the trip pattern
     */
    IntIterator patternStopIterator(int onTripStopPos, int nStopsInPattern);

    /**
     * Create a trip search, to use to find the correct trip to board/alight for
     * a given pattern. This is used to to inject a forward or reverse
     * search into the worker (strategy design pattern).
     *
     * @param timeTable the trip time-table to search
     * @param <T> The TripSchedule type defined by the user of the raptor API.
     * @return The trip search strategy implementation.
     */
    <T extends RaptorTripSchedule> TripScheduleSearch<T> createTripSearch(
            RaptorTimeTable<T> timeTable
    );

    /**
     * Same as {@link #createTripSearch(RaptorTimeTable)}, but create a
     * trip search that only accept exact trip timeLimit matches.
     */
    <T extends RaptorTripSchedule> TripScheduleSearch<T> createExactTripSearch(
            RaptorTimeTable<T> timeTable
    );

    /**
     * Return a calculator for test purpose. The following parameters are fixed:
     * <ul>
     *     <li>'binaryTripSearchThreshold' = 10
     *     <li>'earliestDepartureTime' = 08:00:00
     *     <li>'latestArrivalTime',  = 10:00:00
     *     <li>'iterationStep' = 60 seconds
     * </ul>
     * @param forward if true create a calculator for forward search, if false search
     */
    static TransitCalculator testDummyCalculator(boolean forward) {
        return forward
                ? new ForwardTransitCalculator(
                        10,
                        hm2time(8,0),
                        2 * 60 * 60, // 2 hours
                        TIME_NOT_SET,
                        60
                )
                : new ReverseTransitCalculator(
                        10,
                        hm2time(8,0),
                        2 * 60 * 60, // 2 hours
                        TIME_NOT_SET,
                        60
                );
    }
}
