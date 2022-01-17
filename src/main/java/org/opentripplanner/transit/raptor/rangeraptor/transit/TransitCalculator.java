package org.opentripplanner.transit.raptor.rangeraptor.transit;


import static org.opentripplanner.util.time.TimeUtils.hm2time;

import java.util.Iterator;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * The transit calculator is used to calculate transit related stuff, like calculating
 * <em>earliest boarding time</em> and time-shifting the access paths.
 * <p/>
 * The calculator is shared between the state, worker and path mapping code. This make the
 * calculations consistent and let us hide the request parameters. Hiding the request parameters
 * ensure that this calculator is used.
 * <p>
 * There is one calculator for FORWARD search and one for REVERSE search. The documentation and
 * argument names uses a search-direction agnostic vocabulary. We try to use the terms "source" and
 * "target", in stead of "from/to" and "board/alight".
 * <ul>
 * <li>
 *     In a FORWARD search the "source" means "from" and "TARGET" means "to".
 * </li>
 * <li>
 *     In a BACKWARD search the "source" means "to" and "TARGET" means "from". The traversal of the
 *     graph happens from the destination towards the origin - backwards in time. The "from/to"
 *     refer to the "natural way" we think about a journey, while "source/target" the destination
 *     is the source and the origin is the target in a BACKWARD search.
 * </li>
 * </ul>
 * "Source" and "target" may apply to stop-arrival, trip, board-/aligh-slack, and so on.
 * <p>
 * For a BACKWORD search the "source" means "from" (stop-arrival, trip, and so on).
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface TransitCalculator<T extends RaptorTripSchedule> {

    /**
     * Use this constant to represent an uninitialized time value.
     */
    int TIME_NOT_SET = SearchParams.TIME_NOT_SET;

    /**
     * Return {@code true} is searching forward in space and time, {@code false}
     * if search direction is in reverse.
     */
    boolean searchForward();

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
     * For a forward search return the trip arrival time at stop position including alightSlack.
     * For a reverse search return the next trips departure time at stop position with the
     * boardSlack added.
     *
     * @param trip the current boarded trip
     * @param stopPositionInPattern the stop position/index
     */
    int stopArrivalTime(T trip, int stopPositionInPattern, int slack);

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
     * Forward search: Return {@code true} if the first argument ({@code subject}) is
     * BEFORE the second argument ({@code candidate}). If both are equal {@code false} is returned.
     * <p/>
     * Reverse search: Return {@code true} if the first argument ({@code subject}) is
     * AFTER the second argument ({@code candidate}). If both are equal {@code false} is returned.
     *
     * @return true if subject is better than the candidate; if not false.
     */
    boolean isBefore(int subject, int candidate);

    /**
     * Forward search: Return {@code true} if the first argument ({@code subject}) is
     * AFTER the second argument ({@code candidate}). If both are equal {@code false} is returned.
     * <p/>
     * Reverse search: Return {@code true} if the first argument ({@code subject}) is
     * BEFORE the second argument ({@code candidate}). If both are equal {@code false} is returned.
     *
     * @return true if subject is better than the candidate; if not false.
     */
    boolean isAfter(int subject, int candidate);

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
     *
     * Returns -1 if transfer is not possible after the requested departure time
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
     * Create a trip search, to use to find the correct trip to board/alight for
     * a given pattern. This is used to to inject a forward or reverse
     * search into the worker (strategy design pattern).
     *
     * @param timeTable the trip time-table to search
     * @return The trip search strategy implementation.
     */
    TripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable);

    /**
     * Same as {@link #createTripSearch(RaptorTimeTable)}, but create a
     * trip search that only accept exact trip timeLimit matches.
     */
    TripScheduleSearch<T> createExactTripSearch(RaptorTimeTable<T> timeTable);

    /**
     * Return a transfer provider for the given pattern. When searching forward the
     * given {@code target} is the TO pattern/stop, while when searching in reverse the given
     * target is the FROM pattern/stop.
     */
    RaptorConstrainedTripScheduleBoardingSearch<T> transferConstraintsSearch(RaptorRoute<T> route);

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
    static <T extends RaptorTripSchedule> TransitCalculator<T> testDummyCalculator(boolean forward) {
        return forward
                ? new ForwardTransitCalculator<>(
                        10,
                        hm2time(8,0),
                        2 * 60 * 60, // 2 hours
                        TIME_NOT_SET,
                        60
                )
                : new ReverseTransitCalculator<>(
                        10,
                        hm2time(8,0),
                        2 * 60 * 60, // 2 hours
                        TIME_NOT_SET,
                        60
                );
    }

    /**
     * Return {@code true} if it is allowed/possible to board at a particular stop index, on a
     * normal search. For a backwards search, it checks for alighting instead. This should include
     * checks like: Does the pattern allow boarding at the given stop? Is this accessible to
     * wheelchairs (if requested).
     */
    boolean boardingPossibleAt(RaptorTripPattern pattern, int stopPos);

    /**
     * Same as {@link #boardingPossibleAt(RaptorTripPattern, int)}, but for switched alighting/boarding.
     */
    boolean alightingPossibleAt(RaptorTripPattern pattern, int stopPos);

    /**
     * Returns an iterator over all transfers "from" (or "to" for reverse searches) a stopIndex.
     *
     * @see RaptorTransitDataProvider#getTransfersFromStop(int)
     * @see RaptorTransitDataProvider#getTransfersToStop(int)
     */
    Iterator<? extends RaptorTransfer> getTransfers(RaptorTransitDataProvider<T> transitDataProvider, int fromStop);
}
