package org.opentripplanner.transit.raptor.rangeraptor.standard.heuristics;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.standard.BestNumberOfTransfers;
import org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes.BestTimes;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.util.IntUtils;

import java.util.Collection;
import java.util.function.IntUnaryOperator;


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
    private final Collection<RaptorTransfer> egressLegs;
    private final TransitCalculator calculator;
    private boolean aggregatedResultsCalculated = false;

    private int minJourneyTravelDuration = NOT_SET;
    private int minJourneyNumOfTransfers = NOT_SET;

    public HeuristicsAdapter(
            BestTimes times,
            BestNumberOfTransfers transfers,
            Collection<RaptorTransfer> egressLegs,
            TransitCalculator calculator,
            WorkerLifeCycle lifeCycle
    ) {
        this.times = times;
        this.transfers = transfers;
        this.egressLegs = egressLegs;
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
    public boolean destinationReached() {
        calculateAggregatedResults();
        return minJourneyNumOfTransfers != NOT_SET;
    }

    /**
     * Lazy calculate some of the result values.
     */
    private void calculateAggregatedResults() {
        if(aggregatedResultsCalculated) { return; }

        for (RaptorTransfer it : egressLegs) {
            if(reached(it.stop())) {
                int t = bestTravelDuration(it.stop()) + it.durationInSeconds();
                minJourneyTravelDuration = Math.min(minJourneyTravelDuration, t);

                int n = bestNumOfTransfers(it.stop());
                minJourneyNumOfTransfers = Math.min(minJourneyNumOfTransfers, n);
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
