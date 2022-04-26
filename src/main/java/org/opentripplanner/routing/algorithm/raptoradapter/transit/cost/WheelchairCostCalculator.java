package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static org.opentripplanner.model.WheelChairBoarding.NOT_POSSIBLE;
import static org.opentripplanner.model.WheelChairBoarding.NO_INFORMATION;
import static org.opentripplanner.model.WheelChairBoarding.POSSIBLE;

import javax.annotation.Nonnull;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
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
    wheelchairBoardingCost = new int[3];
    wheelchairBoardingCost[POSSIBLE.ordinal()] = 0;
    wheelchairBoardingCost[NO_INFORMATION.ordinal()] =
      RaptorCostConverter.toRaptorCost(requirements.trips().unknownCost());
    wheelchairBoardingCost[NOT_POSSIBLE.ordinal()] =
      RaptorCostConverter.toRaptorCost(requirements.trips().inaccessibleCost());

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
    int defaultCost = delegate.boardingCost(
      firstBoarding,
      prevArrivalTime,
      boardStop,
      boardTime,
      trip,
      transferConstraints
    );
    int index = trip.wheelchairBoarding().ordinal();
    int wheelchairCost = wheelchairBoardingCost[index];

    return defaultCost + wheelchairCost;
  }

  @Override
  public int onTripRelativeRidingCost(int boardTime, int transitFactorIndex) {
    return delegate.onTripRelativeRidingCost(boardTime, transitFactorIndex);
  }

  @Override
  public int transitArrivalCost(
    int boardCost,
    int alightSlack,
    int transitTime,
    int transitFactorIndex,
    int toStop
  ) {
    return delegate.transitArrivalCost(
      boardCost,
      alightSlack,
      transitTime,
      transitFactorIndex,
      toStop
    );
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
