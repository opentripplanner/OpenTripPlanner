package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.path.ForwardPathMapper;
import org.opentripplanner.transit.raptor.rangeraptor.path.PathMapper;
import org.opentripplanner.transit.raptor.util.IntIterators;
import org.opentripplanner.transit.raptor.util.TimeUtils;

final class ForwardSearchTransitCalculator implements TransitCalculator {
    private final int tripSearchBinarySearchThreshold;
    private final int boardSlackInSeconds;
    private final int earliestDepartureTime;
    private final int searchWindowInSeconds;
    private final int latestAcceptableArrivalTime;
    private final int iterationStep;

    ForwardSearchTransitCalculator(SearchParams s, RaptorTuningParameters t) {
        this(
                t.scheduledTripBinarySearchThreshold(),
                s.boardSlackInSeconds(),
                s.earliestDepartureTime(),
                s.searchWindowInSeconds(),
                s.latestArrivalTime(),
                t.iterationDepartureStepInSeconds()
        );
    }

    ForwardSearchTransitCalculator(
            int tripSearchBinarySearchThreshold,
            int boardSlackInSeconds,
            int earliestDepartureTime,
            int searchWindowInSeconds,
            int latestAcceptableArrivalTime,
            int iterationStep
    ) {
        this.tripSearchBinarySearchThreshold = tripSearchBinarySearchThreshold;
        this.boardSlackInSeconds = boardSlackInSeconds;
        this.earliestDepartureTime = earliestDepartureTime;
        this.searchWindowInSeconds = searchWindowInSeconds;
        this.latestAcceptableArrivalTime = latestAcceptableArrivalTime == TIME_NOT_SET
                ? unreachedTime()
                : latestAcceptableArrivalTime;
        this.iterationStep = iterationStep;
    }

    @Override
    public final int plusDuration(final int time, final int delta) {
        return time + delta;
    }

    @Override
    public final int minusDuration(final int time, final int delta) {
        return time - delta;
    }

    @Override
    public final int duration(final int timeA, final int timeB) {
        return timeB - timeA;
    }

    @Override
    public final int earliestBoardTime(int time) {
        // When searching forward we must add the board slack before we board.
        return addBoardSlack(time);
    }

    @Override
    public final int addBoardSlack(int time) {
        return time + boardSlackInSeconds;
    }

    @Override
    public final int removeBoardSlack(int time) {
        return time - boardSlackInSeconds;
    }

    @Override
    public <T extends RaptorTripSchedule> int stopArrivalTime(T onTrip, int stopPositionInPattern) {
        return onTrip.arrival(stopPositionInPattern);
    }

    @Override
    public final boolean exceedsTimeLimit(int time) {
        return isBest(latestAcceptableArrivalTime, time);
    }

    @Override
    public String exceedsTimeLimitReason() {
        return "The arrival time exceeds the time limit, arrive to late: " +
                TimeUtils.timeToStrLong(latestAcceptableArrivalTime) + ".";
    }

    @Override
    public final boolean isBest(final int subject, final int candidate) {
        return subject < candidate;
    }

    @Override
    public final int originDepartureTime(int firstTransitBoardTime, int accessLegDuration) {
        return firstTransitBoardTime - (boardSlackInSeconds + accessLegDuration);
    }

    @Override
    public final int unreachedTime() {
        return Integer.MAX_VALUE;
    }

    @Override
    public final IntIterator rangeRaptorMinutes() {
        return oneIterationOnly()
                ? IntIterators.singleValueIterator(earliestDepartureTime)
                : IntIterators.intDecIterator(
                        earliestDepartureTime + searchWindowInSeconds,
                        earliestDepartureTime,
                        iterationStep
                );
    }

    @Override
    public boolean oneIterationOnly() {
        return searchWindowInSeconds <= iterationStep;
    }

    @Override
    public final IntIterator patternStopIterator(int nStopsInPattern) {
        return IntIterators.intIncIterator(0, nStopsInPattern);
    }

    @Override
    public final IntIterator patternStopIterator(int onTripStopPos, int nStopsInPattern) {
        // We need to add one, because the input trip is the boarded stop position
        return IntIterators.intIncIterator(onTripStopPos + 1, nStopsInPattern);
    }

    @Override
    public final <T extends RaptorTripSchedule> TripScheduleSearch<T> createTripSearch(
            RaptorTimeTable<T> timeTable
    ) {
        return new TripScheduleBoardSearch<>(tripSearchBinarySearchThreshold, timeTable);
    }

    @Override
    public final <T extends RaptorTripSchedule> TripScheduleSearch<T> createExactTripSearch(
            RaptorTimeTable<T> pattern
    ) {
        return new TripScheduleExactMatchSearch<>(
                createTripSearch(pattern),
                this,
                iterationStep
        );
    }

    @Override
    public final <T extends RaptorTripSchedule> PathMapper<T> createPathMapper() {
        return new ForwardPathMapper<>(this);
    }
}
