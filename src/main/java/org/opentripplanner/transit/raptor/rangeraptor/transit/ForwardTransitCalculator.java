package org.opentripplanner.transit.raptor.rangeraptor.transit;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.transit.GuaranteedTransfer;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TransitArrival;
import org.opentripplanner.transit.raptor.util.IntIterators;
import org.opentripplanner.util.time.TimeUtils;


/**
 * Used to calculate times in a forward trip search.
 */
final class ForwardTransitCalculator<T extends RaptorTripSchedule> implements TransitCalculator<T> {
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
    public int stopArrivalTime(T onTrip, int stopPositionInPattern, int alightSlack) {
        return onTrip.arrival(stopPositionInPattern) + alightSlack;
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
    public final int unreachedTime() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int departureTime(RaptorTransfer transfer, int departureTime) {
        return transfer.earliestDepartureTime(departureTime);
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
    @Nullable
    public Collection<GuaranteedTransfer<T>> guaranteedTransfers(
            RaptorTripPattern<T> toPattern, int toStopPos
    ) {
        return toPattern.listGuaranteedTransfersToPattern(toStopPos);
    }

    @Override
    @Nullable
    public final T findTargetTripInGuarantiedTransfers(
            Collection<GuaranteedTransfer<T>> list,
            TransitArrival<T> fromStopArrival,
            int alightSlack,
            int toStopPos
    ) {
        // We ignore any slack when boarding guaranteed transfers, so we have to revert
        // addition of slack here (the arrival have alightSlack added)
        final int earliestDepartureTime = fromStopArrival.arrivalTime() - alightSlack;

        for (GuaranteedTransfer<T> tx : list) {
            if(tx.fromTripMatches(fromStopArrival)) {
                T toTrip = tx.getToTrip();
                return earliestDepartureTime <= toTrip.departure(toStopPos) ? toTrip : null;
            }
        }
        return null;
    }

    @Override
    public final TripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable) {
        return new TripScheduleBoardingSearch<>(tripSearchBinarySearchThreshold, timeTable);
    }

    @Override
    public final TripScheduleSearch<T> createExactTripSearch(RaptorTimeTable<T> pattern) {
        return new TripScheduleExactMatchSearch<>(
                createTripSearch(pattern),
                this,
                iterationStep
        );
    }

}
