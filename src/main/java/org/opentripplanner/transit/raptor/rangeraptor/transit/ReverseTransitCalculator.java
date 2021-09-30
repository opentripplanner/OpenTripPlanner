package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.IntIterators;
import org.opentripplanner.util.time.TimeUtils;

/**
 * A calculator that will take you back in time not forward, this is the
 * basic logic to implement a reveres search.
 */
final class ReverseTransitCalculator<T extends RaptorTripSchedule> implements TransitCalculator<T> {
    private final int tripSearchBinarySearchThreshold;
    private final int latestArrivalTime;
    private final int searchWindowInSeconds;
    private final int earliestAcceptableDepartureTime;
    private final int iterationStep;

    ReverseTransitCalculator(SearchParams s, RaptorTuningParameters t) {
        // The request is already modified to search backwards, so 'earliestDepartureTime()'
        // goes with destination and 'latestArrivalTime()' match origin.
        this(
                t.scheduledTripBinarySearchThreshold(),
                s.latestArrivalTime(),
                s.searchWindowInSeconds(),
                s.earliestDepartureTime(),
                t.iterationDepartureStepInSeconds()
        );
    }

    ReverseTransitCalculator(
            int binaryTripSearchThreshold,
            int latestArrivalTime,
            int searchWindowInSeconds,
            int earliestAcceptableDepartureTime,
            int iterationStep
    ) {
        this.tripSearchBinarySearchThreshold = binaryTripSearchThreshold;
        this.latestArrivalTime = latestArrivalTime;
        this.searchWindowInSeconds = searchWindowInSeconds;
        this.earliestAcceptableDepartureTime = earliestAcceptableDepartureTime == TIME_NOT_SET
                ? unreachedTime()
                : earliestAcceptableDepartureTime;
        this.iterationStep = iterationStep;
    }

    @Override
    public int plusDuration(final int time, final int duration) {
        // It might seems strange to use minus int the add method, but
        // the "positive" direction in this class is backwards in time;
        // hence we need to subtract the board slack.
        return time - duration;
    }

    @Override
    public int minusDuration(final int time, final int duration) {
        // It might seems strange to use plus int the subtract method, but
        // the "positive" direction in this class is backwards in time;
        // hence we need to add the board slack.
        return time + duration;
    }

    @Override
    public int duration(final int timeA, final int timeB) {
        // When searching in reverse time A is > time B, so to
        // calculate the duration we need to swap A and B
        // compared with the normal forward search
        return timeA - timeB;
    }

    @Override
    public int stopArrivalTime(
            T onTrip,
            int stopPositionInPattern,
            int alightSlack
    ) {
        return plusDuration(onTrip.departure(stopPositionInPattern), alightSlack);
    }

    @Override
    public boolean exceedsTimeLimit(int time) {
        return isBest(earliestAcceptableDepartureTime, time);
    }

    @Override
    public String exceedsTimeLimitReason() {
        return "The departure time exceeds the time limit, depart to early: " +
                TimeUtils.timeToStrLong(earliestAcceptableDepartureTime) + ".";
    }

    @Override
    public boolean isBest(final int subject, final int candidate) {
        // The latest time is the best when searching in reverse
        return subject > candidate;
    }

    @Override
    public int unreachedTime() {
        return Integer.MIN_VALUE;
    }

    @Override
    public int departureTime(RaptorTransfer transfer, int departureTime) {
        return transfer.latestArrivalTime(departureTime);
    }

    @Override
    public IntIterator rangeRaptorMinutes() {
        return oneIterationOnly()
                ? IntIterators.singleValueIterator(latestArrivalTime)
                : IntIterators.intIncIterator(
                        latestArrivalTime - searchWindowInSeconds,
                        latestArrivalTime,
                        iterationStep
                );
    }

    @Override
    public boolean oneIterationOnly() {
        return searchWindowInSeconds <= iterationStep;
    }

    @Override
    public IntIterator patternStopIterator(int nStopsInPattern) {
        return IntIterators.intDecIterator(nStopsInPattern, 0);
    }

    @Override
    public RaptorConstrainedTripScheduleBoardingSearch<T> transferConstraintsSearch(RaptorRoute<T> route) {
        return route.transferConstraintsReverseSearch();
    }

    @Override
    public boolean alightingPossibleAt(RaptorTripPattern pattern, int stopPos) {
        return pattern.boardingPossibleAt(stopPos);
    }

    @Override
    public boolean boardingPossibleAt(RaptorTripPattern pattern, int stopPos) {
        return pattern.alightingPossibleAt(stopPos);
    }

    @Override
    public TripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable) {
        return new TripScheduleAlightSearch<>(tripSearchBinarySearchThreshold, timeTable);
    }

    @Override
    public TripScheduleSearch<T> createExactTripSearch(
            RaptorTimeTable<T> timeTable
    ) {
        return new TripScheduleExactMatchSearch<>(
                createTripSearch(timeTable),
                this,
                -iterationStep
        );
    }
}
