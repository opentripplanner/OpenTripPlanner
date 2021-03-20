package org.opentripplanner.transit.raptor.rangeraptor.standard.heuristics;

import java.util.Collection;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.standard.BestNumberOfTransfers;
import org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes.BestTimes;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.util.IntUtils;
import org.opentripplanner.util.time.TimeUtils;


/**
 * The responsibility of this class is to play the {@link Heuristics} role.
 * It wrap the internal state, and transform the internal model to
 * provide the needed functionality.
 */
public class HeuristicsAdapter implements Heuristics {
    private static final int NOT_SET = Integer.MAX_VALUE;

    private int originDepartureTime = -1;
    private final BestTimes times;
    private final BestNumberOfTransfers transfers;
    private final Collection<RaptorTransfer> egressPaths;
    private final TransitCalculator calculator;
    private boolean aggregatedResultsCalculated = false;

    private int minJourneyTravelDuration = NOT_SET;
    private int minJourneyNumOfTransfers = NOT_SET;
    private int earliestArrivalTime = NOT_SET;

    public HeuristicsAdapter(
            BestTimes times,
            BestNumberOfTransfers transfers,
            Collection<RaptorTransfer> egressPaths,
            TransitCalculator calculator,
            WorkerLifeCycle lifeCycle
    ) {
        this.times = times;
        this.transfers = transfers;
        this.egressPaths = egressPaths;
        this.calculator = calculator;
        lifeCycle.onSetupIteration(this::setUpIteration);
    }

    private void setUpIteration(int departureTime) {
        if (this.originDepartureTime > 0) {
            throw new IllegalStateException(
                    "You should only run one iteration to calculate heuristics, this is because "
                    + "we use the origin departure time to calculate the travel duration at the "
                    + "end of the search.");
        }
        this.originDepartureTime = departureTime;
    }

    @Override
    public boolean reached(int stop) {
        return times.isStopReached(stop);
    }

    @Override
    public int bestTravelDuration(int stop) {
        if(reached(stop)) {
            return calculator.duration(originDepartureTime, times.time(stop));
        }
        return NOT_SET;
    }

    @Override
    public int[] bestTravelDurationToIntArray(int unreached) {
        return toIntArray(size(), unreached, this::bestTravelDuration);
    }

    @Override
    public int bestNumOfTransfers(int stop) {
        return transfers.calculateMinNumberOfTransfers(stop);
    }

    @Override
    public int[] bestNumOfTransfersToIntArray(int unreached) {
        return toIntArray(size(), unreached, this::bestNumOfTransfers);
    }

    @Override
    public int size() {
        return times.size();
    }

    @Override
    public int bestOverallJourneyTravelDuration() {
        calculateAggregatedResults();
        return minJourneyTravelDuration;
    }

    @Override
    public int bestOverallJourneyNumOfTransfers() {
        calculateAggregatedResults();
        return minJourneyNumOfTransfers;
    }

    @Override
    public int minWaitTimeForJourneysReachingDestination() {
        calculateAggregatedResults();
        return Math.abs(earliestArrivalTime - originDepartureTime) - minJourneyTravelDuration;
    }

    @Override
    public boolean destinationReached() {
        calculateAggregatedResults();
        return minJourneyNumOfTransfers != NOT_SET;
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(Heuristics.class)
            .addServiceTime("originDepartureTime(last iteration)", originDepartureTime, NOT_SET)
            .addFieldIfTrue("resultsExist", aggregatedResultsCalculated)
            .addDurationSec("minJourneyTravelDuration", minJourneyTravelDuration, NOT_SET)
            .addDurationSec("minJourneyNumOfTransfers", minJourneyNumOfTransfers, NOT_SET)
            .addServiceTime("earliestArrivalTime", earliestArrivalTime, NOT_SET)
            .addObj("times", times)
            .addCollection(
                "egress stops reached",
                egressPaths.stream()
                    .map(RaptorTransfer::stop)
                    .filter(times::isStopReached)
                    .map(s -> "[" + s + " " + TimeUtils.timeToStrCompact(times.time(s)) + "]")
                    .collect(Collectors.toList()),
                20
            )
            .toString();
    }

    /**
     * Lazy calculate some of the result values.
     */
    private void calculateAggregatedResults() {
        if(aggregatedResultsCalculated) { return; }

        for (RaptorTransfer it : egressPaths) {
            if(reached(it.stop())) {
                int t = bestTravelDuration(it.stop()) + it.durationInSeconds();
                minJourneyTravelDuration = Math.min(minJourneyTravelDuration, t);

                int n = bestNumOfTransfers(it.stop());
                minJourneyNumOfTransfers = Math.min(minJourneyNumOfTransfers, n);

                int eat = times.time(it.stop()) + it.durationInSeconds();
                earliestArrivalTime = Math.min(earliestArrivalTime, eat);
            }
        }
        aggregatedResultsCalculated = true;
    }

    /**
     * Convert one of heuristics to an int array.
     */
    private int[] toIntArray(int size, int unreached, IntUnaryOperator supplier) {
        int[] a = IntUtils.intArray(size, unreached);
        for (int i = 0; i < a.length; i++) {
            if(reached(i)) {
                a[i] = supplier.applyAsInt(i);
            }
        }
        return a;
    }
}
