package org.opentripplanner.transit.raptor.api.transit;


import org.opentripplanner.transit.raptor.api.view.ArrivalView;

/**
 * The responsibility for the cost calculator is to calculate the default  multi-criteria cost.
 * <P/>
 * This class is immutable and thread safe.
 */
public final class DefaultCostCalculator<T extends RaptorTripSchedule> implements CostCalculator<T> {
    private final int boardCostOnly;
    private final int boardAndTransferCost;
    private final int walkFactor;
    private final int waitFactor;
    private final int transitFactor;
    private final int[] stopVisitCost;

    /**
     * Cost unit: SECONDS - The unit for all input parameters are in the OTP TRANSIT model cost unit
     * (in Raptor the unit for cost is centi-seconds).
     *
     * @param stopVisitCost Unit centi-seconds. This parameter is used "as-is" and not transformed
     *                      into the Raptor cast unit to avoid the transformation for each request.
     */
    public DefaultCostCalculator(
            int[] stopVisitCost,
            int boardCost,
            int transferCost,
            double walkReluctanceFactor,
            double waitReluctanceFactor
    ) {
        this.stopVisitCost = stopVisitCost;
        this.boardCostOnly = RaptorCostConverter.toRaptorCost(boardCost);
        this.boardAndTransferCost = RaptorCostConverter.toRaptorCost(transferCost) + boardCostOnly;
        this.walkFactor = RaptorCostConverter.toRaptorCost(walkReluctanceFactor);
        this.waitFactor = RaptorCostConverter.toRaptorCost(waitReluctanceFactor);
        this.transitFactor = RaptorCostConverter.toRaptorCost(1.0);
    }

    @Override
    public final int onTripRidingCost(
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
            + waitFactor * waitTime
            + transitFactor * relativeTransitTime;

        if(stopVisitCost != null) {
            cost += stopVisitCost[previousArrival.stop()];
        }
        return cost;
    }

    @Override
    public final int transitArrivalCost(
        boolean firstRound,
        int fromStop,
        int waitTime,
        int transitTime,
        int toStop
    ) {
        int cost = waitFactor * waitTime + transitFactor * transitTime;

        cost += firstRound ? boardCostOnly : boardAndTransferCost;

        if(stopVisitCost != null) {
            cost += stopVisitCost[fromStop] + stopVisitCost[toStop];
        }
        return cost;
    }

    @Override
    public final int walkCost(int walkTimeInSeconds) {
        return walkFactor * walkTimeInSeconds;
    }

    @Override
    public final int waitCost(int waitTimeInSeconds) {
        return waitFactor * waitTimeInSeconds;
    }

    @Override
    public final int calculateMinCost(int minTravelTime, int minNumTransfers) {
        return  boardCostOnly
            + boardAndTransferCost * minNumTransfers
            + transitFactor * minTravelTime;
    }
}
