package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * The purpose of this class is to optimize the search for a trip schedule for
 * a given pattern and stop. Normally the search scan from the upper bound index
 * and down, it can do so because the trips are ordered after the FIRST stop
 * alight times. We also assume that trips do not pass each other; Hence
 * trips in service on a given day will also be in order for all other stops.
 * For trips operating on different service days (no overlapping) this assumption
 * is not necessary true.
 * <p>
 * The search use a binary search if the number of trip schedules is above a
 * given threshold. A linear search is slow when the number of schedules is very
 * large, let say more than 300 trip schedules.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class TripScheduleAlightSearch<T extends RaptorTripSchedule> implements TripScheduleSearch<T> {
    private final int nTripsBinarySearchThreshold;
    private final RaptorTimeTable<T> timeTable;
    private final int nTrips;

    private int latestAlightTime;
    private int stopPositionInPattern;

    private T candidateTrip;
    private int candidateTripIndex;

    TripScheduleAlightSearch(int scheduledTripBinarySearchThreshold, RaptorTimeTable<T> timeTable) {
        this.nTripsBinarySearchThreshold = scheduledTripBinarySearchThreshold;
        this.timeTable = timeTable;
        this.nTrips = timeTable.numberOfTripSchedules();
    }

    @Override
    public T getCandidateTrip() {
        return candidateTrip;
    }

    @Override
    public int getCandidateTripIndex() {
        return candidateTripIndex;
    }

    @Override
    public int getCandidateTripTime() {
        return candidateTrip.arrival(stopPositionInPattern);
    }

    /**
     * Find the last trip arriving at the given stop BEFORE the given {@code latestAlightTime}.
     * This is the same as calling {@link #search(int, int, int)} with {@code tripIndexLowerBound: -1}.
     *
     * @see #search(int, int, int)
     */
    public boolean search(int latestAlightTime, int stopPositionInPattern) {
        return search(latestAlightTime, stopPositionInPattern, -1);
    }

    /**
     * Find the last trip leaving from the given stop BEFORE the the {@code latestAlightTime}, but after the
     * given trip ({@code tripIndexLowerBound}).
     *
     * @param latestAlightTime      The latest acceptable alight time (exclusive).
     * @param stopPositionInPattern The stop to board.
     * @param tripIndexLowerBound   Upper bound for trip index to search for (exclusive).
     */
    public boolean search(int latestAlightTime, int stopPositionInPattern, int tripIndexLowerBound) {
        this.latestAlightTime = latestAlightTime;
        this.stopPositionInPattern = stopPositionInPattern;
        this.candidateTrip = null;
        this.candidateTripIndex = -1;

        // No previous trip is found
        if (tripIndexLowerBound < 0) {
            if(nTrips > nTripsBinarySearchThreshold) {
                return findFirstBoardingOptimizedForLargeSetOfTrips();
            }
            else {
                return findBoardingSearchForwardInTime(0);
            }
        }
        // We have already found a candidate in a previous search;
        // Hence searching forward from the lower bound is the fastest way to proceed.
        // We have to add 1 to the lower bound for go from exclusive to inclusive
        return findBoardingSearchForwardInTime(tripIndexLowerBound + 1);
    }

    private boolean findFirstBoardingOptimizedForLargeSetOfTrips() {
        int indexBestGuess = binarySearchForTripIndex();

        // Use the best guess from the binary search to look for a candidate trip
        // We can not use upper bound to exit the search. We need to continue
        // until we find a valid trip in service.
        boolean found = findBoardingSearchForwardInTime(indexBestGuess);

        // If a valid result is found and we can return
        if (found) {
            return true;
        }

        // No trip schedule above the best guess was found. This may happen if enough
        // trips are not in service.
        //
        // So we have to search for the first valid trip schedule before that.
        return findBoardingSearchBackwardsInTime(indexBestGuess);
    }

    /**
     * This method search for the last scheduled trip arriving before the {@code latestAlightTime}.
     * Only trips with a trip index greater than the given {@code tripIndexLowerBound} is considered.
     *
     * @param tripIndexLowerBound The trip index lower bound, where search start (inclusive).
     */
    private boolean findBoardingSearchForwardInTime(int tripIndexLowerBound) {
        for (int i = tripIndexLowerBound; i < nTrips;  ++i) {
            T trip = timeTable.getTripSchedule(i);

            final int arrival = trip.arrival(stopPositionInPattern);

            if (arrival <= latestAlightTime) {
                candidateTrip = trip;
                candidateTripIndex = i;
            } else {
                // this trip arrives too early. We can break out of the loop since
                // trips are sorted by departure time (trips in given schedule)
                // Trips passing another trip is not accounted for if both are in service.
                return candidateTrip != null;
            }
        }
        return candidateTrip != null;
    }

    /**
     * This method search for the last scheduled trip arrival before the {@code latestAlightTime}.
     * Only trips with a trip index in the range: {@code [0..tripIndexUpperBound-1]} is considered.
     *
     * @param tripIndexUpperBound The trip index upper bound, where search end (exclusive).
     */
    private boolean findBoardingSearchBackwardsInTime(final int tripIndexUpperBound) {
        for (int i = tripIndexUpperBound-1; i >=0; --i) {
            T trip = timeTable.getTripSchedule(i);

            final int arrival = trip.arrival(stopPositionInPattern);

            if (arrival <= latestAlightTime) {
                candidateTrip = trip;
                candidateTripIndex = i;
                return true;
            }
        }
        return false;
    }

    /**
     * Do a binary search to find the approximate lower bound index for where to start the search.
     * We IGNORE if the trip schedule is in service.
     * <p/>
     * This is just a guess and we return when the trip with a best valid arrival is in the range of
     * the next {@link #nTripsBinarySearchThreshold}.
     *
     * @return a better lower bound index (inclusive)
     */
    private int binarySearchForTripIndex() {
        int lower = 0, upper = nTrips;

        // Do a binary search to find where to start the search.
        // We IGNORE if the trip schedule is in service.
        while (upper - lower > nTripsBinarySearchThreshold) {
            int m = (lower + upper) / 2;

            RaptorTripSchedule trip = timeTable.getTripSchedule(m);

            int arrival = trip.arrival(stopPositionInPattern);

            if (arrival <= latestAlightTime) {
                lower = m;
            }
            else {
                upper = m;
            }
        }
        return lower;
    }
}
