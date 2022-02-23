package org.opentripplanner.transit.raptor.rangeraptor.transit;

import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleSearch;


/**
 * The purpose of this class is to optimize the search for a trip schedule for
 * a given pattern and stop. Normally the search scan from the upper bound index
 * and down, it can do so because the trips are ordered after the FIRST stop
 * boarding times. We also assume that trips do not pass each other; Hence
 * trips IN SERVICE on a given day will be in order for all other stops too.
 * <p/>
 * The search use a binary search if the number of trip schedules is above a
 * given threshold. A linear search is slow when the number of schedules is very
 * large, let say more than 300 trip schedules.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TripScheduleBoardSearch<T extends RaptorTripSchedule>
        implements RaptorTripScheduleSearch<T>, RaptorTripScheduleBoardOrAlightEvent<T>
{
    private static final int NOT_SET = -1;

    private final int nTripsBinarySearchThreshold;
    private final RaptorTimeTable<T> timeTable;
    private final int nTrips;

    private int earliestBoardTime;
    private int stopPositionInPattern;
    private IntUnaryOperator departureTimes;

    private T candidateTrip;
    private int candidateTripIndex = NOT_SET;

    TripScheduleBoardSearch(
            int scheduledTripBinarySearchThreshold,
            RaptorTimeTable<T> timeTable
    ) {
        this.nTripsBinarySearchThreshold = scheduledTripBinarySearchThreshold;
        this.timeTable = timeTable;
        this.nTrips = timeTable.numberOfTripSchedules();
    }


    /* TripScheduleBoardOrAlightEvent implementation using fly-weight pattern */

    @Override
    public T getTrip() {
        return candidateTrip;
    }

    @Override
    public int getTripIndex() {
        return candidateTripIndex;
    }

    @Override
    public int getTime() {
        return candidateTrip.departure(stopPositionInPattern);
    }

    @Override
    public int getStopPositionInPattern() {
        return stopPositionInPattern;
    }

    @Override
    public RaptorTransferConstraint getTransferConstraint() {
        return RaptorTransferConstraint.REGULAR_TRANSFER;
    }


    /* TripScheduleSearch implementation */

    /**
     * Find the first trip leaving from the given stop AFTER the the 'earliestTime', but
     * before the given trip ({@code tripIndexUpperBound}).
     *
     * @param earliestTime     The time of arrival at the given stop for the previous trip.
     * @param stopPositionInPattern The stop to board
     * @param tripIndexUpperBound   Upper bound for trip index to search for. Exclusive - search start
     *                              at {@code tripIndexUpperBound - 1}.
     *                              Use {@code -1} (negative value) for an unbounded search.
     */
    @Override
    public RaptorTripScheduleBoardOrAlightEvent<T> search(
            int earliestTime,
            int stopPositionInPattern,
            int tripIndexUpperBound
    ) {
        this.earliestBoardTime = earliestTime;
        this.stopPositionInPattern = stopPositionInPattern;
        this.departureTimes = timeTable.getDepartureTimes(stopPositionInPattern);
        this.candidateTrip = null;
        this.candidateTripIndex = NOT_SET;

        // No previous trip is found
        if (tripIndexUpperBound == UNBOUNDED_TRIP_INDEX) {
            if (nTrips > nTripsBinarySearchThreshold) {
                return findFirstBoardingOptimizedForLargeSetOfTrips();
            }
            else {
                return findBoardingBySteppingBackwardsInTime(nTrips);
            }
        }
        // We have already found a candidate in a previous search;
        // Hence searching reverse from the upper bound is the fastest way to proceed.
        return findBoardingBySteppingBackwardsInTime(tripIndexUpperBound);
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(TripScheduleBoardSearch.class)
                .addObj("nTrips", nTrips)
                .addObj("earliestBoardTime", earliestBoardTime)
                .addObj("stopPos", stopPositionInPattern)
                .addObj("tripIndex", candidateTripIndex)
                .addObj("trip", candidateTrip)
                .toString();
    }


    /* private methods */

    private RaptorTripScheduleBoardOrAlightEvent<T> findFirstBoardingOptimizedForLargeSetOfTrips() {
        int indexBestGuess = binarySearchForTripIndex();

        // Use the upper bound from the binary search to look for a candidate trip
        // We can not use lower bound to exit the search. We need to continue
        // until we find a valid trip in service.
        var result = findBoardingBySteppingBackwardsInTime(indexBestGuess);

        // If a valid result is found and we can return
        if (result != null) {
            return this;
        }

        // No trip schedule below the best guess was found. This may happen if enough
        // trips are not in service.
        //
        // So we have to search for the first valid trip schedule after that.
        return findBoardingBySteppingForwardInTime(indexBestGuess);
    }

    /**
     * This method search for the first scheduled trip boarding, after or equals to the
     * given {@code earliestBoardTime}. Only trips with a trip index smaller than the given
     * {@code tripIndexUpperBound} is considered.
     * <p/>
     * The search searches backwards until index 0 is reached (inclusive).
     *
     * @param tripIndexUpperBound The trip index upper bound, where search start (exclusive).
     */
    @Nullable
    private RaptorTripScheduleBoardOrAlightEvent<T> findBoardingBySteppingBackwardsInTime(int tripIndexUpperBound) {
        for (int i = tripIndexUpperBound-1; i >= 0; --i) {
            if (departureTimes.applyAsInt(i) >= earliestBoardTime) {
                candidateTripIndex = i;
            } else {
                // this trip arrives too early. We can break out of the loop since
                // trips are sorted by departure time (trips in given schedule)
                // Trips passing another trip is not accounted for if both are in service.
                break;
            }
        }
        if (candidateTripIndex == NOT_SET) { return null; }
        candidateTrip = timeTable.getTripSchedule(candidateTripIndex);
        return this;
    }


    /**
     * This method search for the first scheduled trip boarding, after or equals to
     * the given {@code earliestBoardTime}.
     *
     * @param tripIndexLowerBound The trip index lower bound, where search start (inclusive).
     */
    @Nullable
    private RaptorTripScheduleBoardOrAlightEvent<T> findBoardingBySteppingForwardInTime(final int tripIndexLowerBound) {
        for (int i = tripIndexLowerBound; i < nTrips; ++i) {
            if (departureTimes.applyAsInt(i) >= earliestBoardTime) {
                candidateTrip = timeTable.getTripSchedule(i);
                candidateTripIndex = i;
                return this;
            }
        }
        return null;
    }

    /**
     * Do a binary search to find the approximate upper bound index for where to start the search.
     * <p/>
     * This is just a guess and we return when the trip with a best valid departure is in the range
     * of the next {@link #nTripsBinarySearchThreshold}.
     *
     * @return a better upper bound index (exclusive)
     */
    private int binarySearchForTripIndex() {
        int lower = 0, upper = nTrips;

        // Do a binary search to find where to start the search.
        while (upper - lower > nTripsBinarySearchThreshold) {
            int m = (lower + upper) / 2;

            if (departureTimes.applyAsInt(m) >= earliestBoardTime) {
                upper = m;
            }
            else {
                lower = m;
            }
        }
        return upper == nTrips ? nTrips : upper + 1;
    }
}
