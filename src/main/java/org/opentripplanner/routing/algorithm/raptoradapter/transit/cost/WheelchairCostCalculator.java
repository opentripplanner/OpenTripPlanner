package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static org.opentripplanner.model.WheelChairBoarding.NOT_POSSIBLE;
import static org.opentripplanner.model.WheelChairBoarding.NO_INFORMATION;
import static org.opentripplanner.model.WheelChairBoarding.POSSIBLE;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public class WheelchairCostCalculator implements CostCalculator {

    private final CostCalculator delegate;
    private final int[] wheelchairBoardingCost;

    public WheelchairCostCalculator(
            @Nonnull CostCalculator delegate,
            @Nonnull WheelchairAccessibilityRequest requirements
    ) {
        // assign the costs for boarding a trip with the following accessibility values
        wheelchairBoardingCost = Map.of(
                        NO_INFORMATION, RaptorCostConverter.toRaptorCost(requirements.trips().unknownCost()),
                        POSSIBLE, 0,
                        NOT_POSSIBLE, RaptorCostConverter.toRaptorCost(requirements.trips().inaccessibleCost())
                )
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(k -> k.getKey().ordinal()))
                .mapToInt(Entry::getValue)
                .toArray();

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
        int defaultCost =
                delegate.boardingCost(firstBoarding, prevArrivalTime, boardStop, boardTime, trip,
                        transferConstraints
                );
        var tripSchedule = (TripSchedule) trip;
        int index = tripSchedule.getOriginalTripTimes().getTrip().getWheelchairBoarding().ordinal();
        int wheelchairCost = wheelchairBoardingCost[index];

        return defaultCost + wheelchairCost;
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
