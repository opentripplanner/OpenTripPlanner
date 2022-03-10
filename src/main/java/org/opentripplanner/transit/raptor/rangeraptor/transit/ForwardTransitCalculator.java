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
 * Used to calculate times in a forward trip search.
 */
final class ForwardTransitCalculator<T extends RaptorTripSchedule>
        extends ForwardTimeCalculator
        implements TransitCalculator<T>
{
    private final int tripSearchBinarySearchThreshold;
    private final int earliestDepartureTime;
    private final int searchWindowInSeconds;
    private final int latestAcceptableArrivalTime;
    private final int iterationStep;

    ForwardTransitCalculator(SearchParams s, RaptorTuningParameters t) {
        this(
                t.scheduledTripBinarySearchThreshold(),
                s.earliestDepartureTime(),
                s.searchWindowInSeconds(),
                s.latestArrivalTime(),
                t.iterationDepartureStepInSeconds()
        );
    }

    ForwardTransitCalculator(
            int tripSearchBinarySearchThreshold,
            int earliestDepartureTime,
            int searchWindowInSeconds,
            int latestAcceptableArrivalTime,
            int iterationStep
    ) {
        this.tripSearchBinarySearchThreshold = tripSearchBinarySearchThreshold;
        this.earliestDepartureTime = earliestDepartureTime;
        this.searchWindowInSeconds = searchWindowInSeconds;
        this.latestAcceptableArrivalTime = latestAcceptableArrivalTime == TIME_NOT_SET
                ? unreachedTime()
                : latestAcceptableArrivalTime;
        this.iterationStep = iterationStep;
    }

    @Override
    public int stopArrivalTime(T onTrip, int stopPositionInPattern, int alightSlack) {
        return onTrip.arrival(stopPositionInPattern) + alightSlack;
    }

    @Override
    public boolean exceedsTimeLimit(int time) {
        return isBefore(latestAcceptableArrivalTime, time);
    }

    @Override
    public String exceedsTimeLimitReason() {
        return "The arrival time exceeds the time limit, arrive to late: " +
                TimeUtils.timeToStrLong(latestAcceptableArrivalTime) + ".";
    }

    @Override
    public int departureTime(RaptorTransfer transfer, int departureTime) {
        return transfer.earliestDepartureTime(departureTime);
    }

    @Override
    public IntIterator rangeRaptorMinutes() {
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
    public IntIterator patternStopIterator(int nStopsInPattern) {
        return IntIterators.intIncIterator(0, nStopsInPattern);
    }

    @Override
    public RaptorConstrainedTripScheduleBoardingSearch<T> transferConstraintsSearch(RaptorRoute<T> route) {
        return route.transferConstraintsForwardSearch();
    }

    @Override
    public boolean alightingPossibleAt(RaptorTripPattern pattern, int stopPos) {
        return pattern.alightingPossibleAt(stopPos);
    }

    @Override
    public Iterator<? extends RaptorTransfer> getTransfers(RaptorTransitDataProvider<T> transitDataProvider, int fromStop) {
        return transitDataProvider.getTransfersFromStop(fromStop);
    }

    @Override
    public boolean boardingPossibleAt(RaptorTripPattern pattern, int stopPos) {
        return pattern.boardingPossibleAt(stopPos);
    }

    @Override
    public RaptorTripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable) {
        if (timeTable.useCustomizedTripSearch()) {
            return timeTable.createCustomizedTripSearch(SearchDirection.FORWARD);
        }
        return new TripScheduleBoardSearch<>(tripSearchBinarySearchThreshold, timeTable);
    }

    @Override
    public RaptorTripScheduleSearch<T> createExactTripSearch(RaptorTimeTable<T> pattern) {
        return new TripScheduleExactMatchSearch<>(
                createTripSearch(pattern),
                this,
                iterationStep
        );
    }
}
