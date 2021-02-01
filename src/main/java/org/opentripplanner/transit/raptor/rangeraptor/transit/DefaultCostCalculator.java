package org.opentripplanner.transit.raptor.rangeraptor.transit;


import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

/**
 * The responsibility for the cost calculator is to calculate the default  multi-criteria cost.
 * <P/>
 * This class is immutable and thread safe.
 */
public class DefaultCostCalculator<T extends RaptorTripSchedule> implements CostCalculator<T> {
    private final int boardCost;
    private final int transferCost;
    private final int walkFactor;
    private final int waitFactor;
    private final int transitFactor;
    private final int[] stopVisitCost;

    /**
     * This is the combined cost of transfer and boarding. In round 0 and 1 this is
     * just set to the boarding cost, while for rounds 2 and up this is the sum of both.
     */
    private int transferAndBoardCost;

    /**
     * We only apply the wait factor between transits, not between access and transit;
     * Hence we start with 0 (zero) and after the first round we set this to the
     * provided {@link #waitFactor}. We assume we can time-shift the access to get rid
     * of the wait time.
     */
    private int waitFactorApplied = 0;


    public DefaultCostCalculator(
            int[] stopVisitCost,
            int boardCost,
            int transferCost,
            double walkReluctanceFactor,
            double waitReluctanceFactor,
            WorkerLifeCycle lifeCycle
    ) {
        this.stopVisitCost = stopVisitCost;
        this.boardCost = RaptorCostConverter.toRaptorCost(boardCost);
        this.transferCost = RaptorCostConverter.toRaptorCost(transferCost);
        this.walkFactor = RaptorCostConverter.toRaptorCost(walkReluctanceFactor);
        this.waitFactor = RaptorCostConverter.toRaptorCost(waitReluctanceFactor);
        this.transitFactor = RaptorCostConverter.toRaptorCost(1.0);
        lifeCycle.onPrepareForNextRound(this::prepareForNewRound);
    }

    @Override
    public int onTripRidingCost(
        ArrivalView<T> previousArrival,
        int waitTime,
        int boardTime
    ) {
        // The relative-transit-time is time spent on transit. We do not know the alight-stop, so
        // it is impossible to calculate the "correct" time. But the only thing that maters is that
        // the relative difference between to boardings are correct, assuming riding the same trip.
        // So, we can use the negative board time as relative-transit-time.
        final int relativeTransitTime =  -boardTime;

        // No need to add board/transfer cost here, since all "onTripRide"s have the same
        // board/transfer cost.
        int cost = previousArrival.cost()
            + waitFactorApplied * waitTime
            + transitFactor * relativeTransitTime;

        if(stopVisitCost != null) {
            cost += stopVisitCost[previousArrival.stop()];
        }
        return cost;
    }

    @Override
    public int transitArrivalCost(
        int fromStop,
        int waitTime,
        int transitTime,
        int toStop,
        T trip
    ) {
        int cost = waitFactorApplied * waitTime + transitFactor * transitTime + transferAndBoardCost;
        if(stopVisitCost != null) {
            cost += stopVisitCost[fromStop] + stopVisitCost[toStop];
        }
        return cost;
    }

    @Override
    public int walkCost(int walkTimeInSeconds) {
        return walkFactor * walkTimeInSeconds;
    }

    @Override
    public int waitCost(int waitTimeInSeconds) {
        return waitFactor * waitTimeInSeconds;
    }

    @Override
    public int calculateMinCost(int minTravelTime, int minNumTransfers) {
        return  boardCost * (minNumTransfers + 1)
            + transferCost * minNumTransfers
            + transitFactor * minTravelTime;
    }

    private void prepareForNewRound(int round) {
        // Access(0) and first transit+transfer
        if(round < 2) {
            // For access(round 0) and the first transit round(1) skip adding a cost for waiting,
            // we assume we can time-shift the access path.
            this.waitFactorApplied = 0;
            // Only add board cost, not transferCost
            this.transferAndBoardCost = boardCost;
        }
        else {
            this.waitFactorApplied = waitFactor;
            this.transferAndBoardCost = boardCost + transferCost;
        }
    }
}
