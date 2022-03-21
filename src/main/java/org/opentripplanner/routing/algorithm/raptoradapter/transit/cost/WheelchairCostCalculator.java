package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public class WheelchairCostCalculator implements CostCalculator {

    private final CostCalculator delegate;
    private final int[] wheelchairBoardingCost = new int[]{600, 0, 3600};

    public WheelchairCostCalculator(CostCalculator delegate) {
        this.delegate = delegate;
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
        int cost = delegate.boardingCost(firstBoarding, prevArrivalTime, boardStop, boardTime, trip,
                transferConstraints
        );
        var tripSchedule = (TripSchedule) trip;
        int index = tripSchedule.getOriginalTripTimes().getTrip().getWheelchairBoarding().ordinal();
        int wheelchairCost = wheelchairBoardingCost[index];

        return cost + wheelchairCost;
    }

    @Override
    public int onTripRelativeRidingCost(int boardTime, int transitFactorIndex) {
        return delegate.onTripRelativeRidingCost(boardTime, transitFactorIndex);
    }

    @Override
    public int transitArrivalCost(
            int boardCost, int alightSlack, int transitTime, int transitFactorIndex, int toStop
    ) {
        return delegate.transitArrivalCost(
                boardCost, alightSlack, transitTime, transitFactorIndex, toStop);
    }

    @Override
    public int waitCost(int waitTimeInSeconds) {
        return delegate.waitCost(waitTimeInSeconds);
    }

    @Override
    public int calculateMinCost(int minTravelTime, int minNumTransfers) {
        return delegate.calculateMinCost(minTravelTime, minNumTransfers);
    }

    @Override
    public int costEgress(RaptorTransfer egress) {
        return delegate.costEgress(egress);
    }
}
