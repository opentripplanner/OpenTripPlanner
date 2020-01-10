package org.opentripplanner.transit.raptor.rangeraptor.standard.heuristics;

import org.opentripplanner.transit.raptor.api.transit.TransferLeg;
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
    private final Collection<TransferLeg> transferLegs;
    private final TransitCalculator calculator;

    private int minJourneyTravelDuration = NOT_SET;
    private int minJourneyNumOfTransfers = NOT_SET;

    public HeuristicsAdapter(
            BestTimes times,
            BestNumberOfTransfers transfers,
            Collection<TransferLeg> transferLegs,
            TransitCalculator calculator,
            WorkerLifeCycle lifeCycle
    ) {
        this.times = times;
        this.transfers = transfers;
        this.transferLegs = transferLegs;
        this.calculator = calculator;
        lifeCycle.onSetupIteration(this::setUpIteration);
    }

    private void setUpIteration(int departureTime) {
        if (this.originDepartureTime > 0) {
            throw new IllegalStateException(
                    "You should only run one iteration to calculate heuristics, this is because we use " +
                    "the origin departure time to calculate the travel duration at the end of the search."
            );
        }
        this.originDepartureTime = departureTime;
    }

    @Override
    public boolean reached(int stop) {
        return times.isStopReached(stop);
    }

    @Override
    public int bestTravelDuration(int stop) {
        return calculator.duration(originDepartureTime, times.time(stop));
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
        if(minJourneyTravelDuration == NOT_SET) {
            for (TransferLeg it : transferLegs) {
                int v = bestTravelDuration(it.stop()) + it.durationInSeconds();
                minJourneyTravelDuration = Math.min(minJourneyTravelDuration, v);
            }
        }
        return minJourneyTravelDuration;
    }

    @Override
    public int bestOverallJourneyNumOfTransfers() {
        if(minJourneyNumOfTransfers == NOT_SET) {
            for (TransferLeg it : transferLegs) {
                int v = bestNumOfTransfers(it.stop());
                minJourneyNumOfTransfers = Math.min(minJourneyNumOfTransfers, v);
            }
        }
        return minJourneyNumOfTransfers;
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
