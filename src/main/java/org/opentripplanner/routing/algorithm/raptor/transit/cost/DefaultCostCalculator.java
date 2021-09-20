package org.opentripplanner.routing.algorithm.raptor.transit.cost;


import javax.annotation.Nullable;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;

/**
 * The responsibility for the cost calculator is to calculate the default  multi-criteria cost.
 * <P/>
 * This class is immutable and thread safe.
 */
public final class DefaultCostCalculator implements CostCalculator {
    private final int boardCostOnly;
    private final int boardAndTransferCost;
    private final int waitFactor;
    private final FactorStrategy transitFactors;
    private final int[] stopVisitCost;


    /**
     * Cost unit: SECONDS - The unit for all input parameters are in the OTP TRANSIT model cost unit
     * (in Raptor the unit for cost is centi-seconds).
     *
     * @param stopVisitCost Unit centi-seconds. This parameter is used "as-is" and not transformed
     *                      into the Raptor cast unit to avoid the transformation for each request.
     *                      Use {@code null} to ignore stop cost.
     */
    public DefaultCostCalculator(
            int boardCost,
            int transferCost,
            double waitReluctanceFactor,
            @Nullable double[] transitReluctanceFactors,
            @Nullable int[] stopVisitCost
    ) {
        this.boardCostOnly = RaptorCostConverter.toRaptorCost(boardCost);
        this.boardAndTransferCost = RaptorCostConverter.toRaptorCost(transferCost) + boardCostOnly;
        this.waitFactor = RaptorCostConverter.toRaptorCost(waitReluctanceFactor);

        this.transitFactors = transitReluctanceFactors == null
            ? new SingleValueFactorStrategy(McCostParams.DEFAULT_TRANSIT_RELUCTANCE)
            : new IndexBasedFactorStrategy(transitReluctanceFactors);

        this.stopVisitCost = stopVisitCost;
    }

    public DefaultCostCalculator(McCostParams params, int[] stopVisitCost) {
        this(
                params.boardCost(),
                params.transferCost(),
                params.waitReluctanceFactor(),
                params.transitReluctanceFactors(),
                stopVisitCost
        );
    }

    @Override
    public final int boardCost(
            boolean firstRide,
            int waitTime,
            int boardStop
    ) {
        int cost = waitFactor * waitTime;

        cost += firstRide ? boardCostOnly : boardAndTransferCost;

        if(stopVisitCost != null) {
            cost += stopVisitCost[boardStop];
        }
        return cost;
    }

    @Override
    public final int onTripRelativeRidingCost(
            int boardTime,
            int transitFactorIndex
    ) {
        // The relative-transit-time is time spent on transit. We do not know the alight-stop, so
        // it is impossible to calculate the "correct" time. But the only thing that maters is that
        // the relative difference between to boardings are correct, assuming riding the same trip.
        // So, we can use the negative board time as relative-transit-time.
        return -boardTime * transitFactors.factor(transitFactorIndex);
    }

    @Override
    public final int transitArrivalCost(
        int boardCost,
        int alightSlack,
        int transitTime,
        int transitFactorIndex,
        int toStop
    ) {
        int cost = boardCost
                + transitFactors.factor(transitFactorIndex) * transitTime
                + waitFactor * alightSlack;

        if(stopVisitCost != null) {
            cost += stopVisitCost[toStop];
        }

        return cost;
    }

    @Override
    public final int waitCost(int waitTimeInSeconds) {
        return waitFactor * waitTimeInSeconds;
    }

    @Override
    public final int calculateMinCost(int minTravelTime, int minNumTransfers) {
        return  boardCostOnly
            + boardAndTransferCost * minNumTransfers
            + transitFactors.minFactor() * minTravelTime;
    }
}
