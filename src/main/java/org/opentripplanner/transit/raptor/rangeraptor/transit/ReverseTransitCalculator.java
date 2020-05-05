package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.IntIterators;
import org.opentripplanner.transit.raptor.util.TimeUtils;

/**
 * A calculator that will take you back in time not forward, this is the
 * basic logic to implement a reveres search.
 */
final class ReverseTransitCalculator implements TransitCalculator {
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
    public final int plusDuration(final int time, final int duration) {
        // It might seems strange to use minus int the add method, but
        // the "positive" direction in this class is backwards in time;
        // hence we need to subtract the board slack.
        return time - duration;
    }

    @Override
    public final int minusDuration(final int time, final int duration) {
        // It might seems strange to use plus int the subtract method, but
        // the "positive" direction in this class is backwards in time;
        // hence we need to add the board slack.
        return time + duration;
    }

    @Override
    public final int duration(final int timeA, final int timeB) {
        // When searching in reverse time A is > time B, so to
        // calculate the duration we need to swap A and B
        // compared with the normal forward search
        return timeA - timeB;
    }

    @Override
    public final <T extends RaptorTripSchedule> int stopArrivalTime(
            T onTrip,
            int stopPositionInPattern,
            int alightSlack
    ) {
        return plusDuration(onTrip.departure(stopPositionInPattern), alightSlack);
    }

    @Override
    public final boolean exceedsTimeLimit(int time) {
        return isBest(earliestAcceptableDepartureTime, time);
    }

    @Override
    public final String exceedsTimeLimitReason() {
        return "The departure time exceeds the time limit, depart to early: " +
                TimeUtils.timeToStrLong(earliestAcceptableDepartureTime) + ".";
    }

    @Override
    public final boolean isBest(final int subject, final int candidate) {
        // The latest time is the best when searching in reverse
        return subject > candidate;
    }

    @Override
    public final int unreachedTime() {
        return Integer.MIN_VALUE;
    }

    @Override
    public int departureTime(RaptorTransfer transfer, int departureTime) {
        return transfer.latestArrivalTime(departureTime);
    }

    @Override
    public final IntIterator rangeRaptorMinutes() {
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
    public final IntIterator patternStopIterator(int nStopsInPattern) {
        return IntIterators.intDecIterator(nStopsInPattern, 0);
    }

    @Override
    public final IntIterator patternStopIterator(int onTripStopPos, int nStopsInPattern) {
        return IntIterators.intDecIterator(onTripStopPos, 0);
    }

    @Override
    public final <T extends RaptorTripSchedule> TripScheduleSearch<T> createTripSearch(
            RaptorTimeTable<T> timeTable
    ) {
        return new TripScheduleAlightSearch<>(tripSearchBinarySearchThreshold, timeTable);
    }

    @Override
    public final <T extends RaptorTripSchedule> TripScheduleSearch<T> createExactTripSearch(
            RaptorTimeTable<T> timeTable
    ) {
        return new TripScheduleExactMatchSearch<>(
                createTripSearch(timeTable),
                this,
                -iterationStep
        );
    }
}
