package org.opentripplanner.transit.raptor.rangeraptor.transit;

import java.util.Iterator;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleSearch;
import org.opentripplanner.transit.raptor.util.IntIterators;
import org.opentripplanner.util.time.TimeUtils;

/**
 * A calculator that will take you back in time not forward, this is the
 * basic logic to implement a reveres search.
 */
final class ReverseTransitCalculator<T extends RaptorTripSchedule>
        extends ReverseTimeCalculator
        implements TransitCalculator<T>
{
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
    public int stopArrivalTime(
            T onTrip,
            int stopPositionInPattern,
            int alightSlack
    ) {
        return plusDuration(onTrip.departure(stopPositionInPattern), alightSlack);
    }

    @Override
    public boolean exceedsTimeLimit(int time) {
        return isBefore(earliestAcceptableDepartureTime, time);
    }

    @Override
    public String exceedsTimeLimitReason() {
        return "The departure time exceeds the time limit, depart to early: " +
                TimeUtils.timeToStrLong(earliestAcceptableDepartureTime) + ".";
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
    public Iterator<? extends RaptorTransfer> getTransfers(RaptorTransitDataProvider<T> transitDataProvider, int fromStop) {
        return transitDataProvider.getTransfersToStop(fromStop);
    }

    @Override
    public boolean boardingPossibleAt(RaptorTripPattern pattern, int stopPos) {
        return pattern.alightingPossibleAt(stopPos);
    }

    @Override
    public RaptorTripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable) {
        if (timeTable.useCustomizedTripSearch()) {
            return timeTable.createCustomizedTripSearch(SearchDirection.REVERSE);
        }

        return new TripScheduleAlightSearch<>(tripSearchBinarySearchThreshold, timeTable);
    }

    @Override
    public RaptorTripScheduleSearch<T> createExactTripSearch(
            RaptorTimeTable<T> timeTable
    ) {
        return new TripScheduleExactMatchSearch<>(
                createTripSearch(timeTable),
                this,
                -iterationStep
        );
    }
}
