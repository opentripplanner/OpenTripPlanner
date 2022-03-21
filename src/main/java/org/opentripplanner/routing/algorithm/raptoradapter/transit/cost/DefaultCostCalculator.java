package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;


import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * The responsibility for the cost calculator is to calculate the default  multi-criteria cost.
 * <P/>
 * This class is immutable and thread safe.
 */
public final class DefaultCostCalculator implements CostCalculator {
    private final int boardCostOnly;
    private final int transferCostOnly;
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
        this.transferCostOnly = RaptorCostConverter.toRaptorCost(transferCost);
        this.boardAndTransferCost = transferCostOnly + boardCostOnly;
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
    public int boardingCost(
            boolean firstBoarding,
            int prevArrivalTime,
            int boardStop,
            int boardTime,
            RaptorTripSchedule trip,
            RaptorTransferConstraint transferConstraints
    ) {
        if(transferConstraints.isRegularTransfer()) {
            return boardingCostRegularTransfer(
                    firstBoarding,
                    prevArrivalTime,
                    boardStop,
                    boardTime
            );
        }
        else {
            return boardingCostConstrainedTransfer(
                    prevArrivalTime,
                    boardStop,
                    boardTime,
                    trip.transitReluctanceFactorIndex(),
                    firstBoarding,
                    transferConstraints
            );
        }
    }

    @Override
    public int onTripRelativeRidingCost(
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
    public int transitArrivalCost(
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
    public int waitCost(int waitTimeInSeconds) {
        return waitFactor * waitTimeInSeconds;
    }

    @Override
    public int calculateMinCost(int minTravelTime, int minNumTransfers) {
        return  boardCostOnly
            + boardAndTransferCost * minNumTransfers
            + transitFactors.minFactor() * minTravelTime;
    }

    @Override
    public int costEgress(RaptorTransfer egress) {
        return egress.hasRides()
                ? egress.generalizedCost() + transferCostOnly
                : egress.generalizedCost();
    }

    /** This is public for test purposes only */
    public int boardingCostRegularTransfer(
            boolean firstBoarding,
            int prevArrivalTime,
            int boardStop,
            int boardTime
    ) {
        // Calculate the wait-time before the boarding which should be accounted for in the cost
        // calculation. Any slack at the end of the last leg is not part of this, because it is
        // already accounted for. If the previous leg is an access leg, then it is already
        // time-shifted, which is important for this calculation to be correct.
        final int boardWaitTime = boardTime - prevArrivalTime;

        int cost = waitFactor * boardWaitTime;

        cost += firstBoarding ? boardCostOnly : boardAndTransferCost;

        if(stopVisitCost != null) {
            cost += stopVisitCost[boardStop];
        }
        return cost;
    }


    /* private methods */

    private int boardingCostConstrainedTransfer(
            int prevArrivalTime,
            int boardStop,
            int boardTime,
            int transitReluctanceIndex,
            boolean firstBoarding,
            RaptorTransferConstraint txConstraints
    ) {
        // This cast could be avoided, if we added another generic type to the Raptor component,
        // but it would be rather messy, just to avoid a single cast.
        var tx = (TransferConstraint) txConstraints;

        if(tx.isStaySeated()) {
            final int boardWaitTime = boardTime - prevArrivalTime;
            int transitReluctance = transitFactors.factor(transitReluctanceIndex);
            // For a stay-seated transfer the wait-time is spent on-board and we should use the
            // transitReluctance, not the waitReluctance, to find the cost of the time since
            // the stop arrival. So we take the time and multiply it with the transit reluctance.
            //
            // Note! if the boarding happens BEFORE the previous stop arrival, we will get a
            // negative time - this is ok, so we allow it in this calculation.
            //
            // The previous stop arrival might have a small alight-slack, this should be replaced
            // with "on-board" time, but the slack should be short and the differance between
            // transit reluctance and wait reluctance is also small, so we ignore this.
            //
            return transitReluctance * boardWaitTime;
        }
        else if(tx.isGuaranteed()) {
            // For a guaranteed transfer we skip board- and transfer-cost
            final int boardWaitTime = boardTime - prevArrivalTime;

            int cost = waitFactor * boardWaitTime;

            if(stopVisitCost != null) {
                cost += stopVisitCost[boardStop];
            }
            return cost;
        }
        // fallback to regular transfer
        return boardingCostRegularTransfer(firstBoarding, prevArrivalTime, boardStop, boardTime);
    }
}
