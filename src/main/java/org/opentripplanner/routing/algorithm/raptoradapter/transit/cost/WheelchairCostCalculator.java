package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import javax.annotation.Nonnull;
import org.opentripplanner.model.WheelchairAccessibility;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;

public class WheelchairCostCalculator<T extends DefaultTripSchedule> implements CostCalculator<T> {

  private final CostCalculator<T> delegate;
  private final int[] wheelchairBoardingCost;

  public WheelchairCostCalculator(
    @Nonnull CostCalculator<T> delegate,
    @Nonnull WheelchairAccessibilityRequest requirements
  ) {
    // assign the costs for boarding a trip with the following accessibility values
    wheelchairBoardingCost = new int[WheelchairAccessibility.values().length];
    for (var it : WheelchairAccessibility.values()) {
      setWheelchairCost(wheelchairBoardingCost, it, requirements);
    }

    this.delegate = delegate;
  }

  @Override
  public int boardingCost(
    boolean firstBoarding,
    int prevArrivalTime,
    int boardStop,
    int boardTime,
    T trip,
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
  public int onTripRelativeRidingCost(int boardTime, T tripScheduledBoarded) {
    return delegate.onTripRelativeRidingCost(boardTime, tripScheduledBoarded);
  }

  @Override
  public int transitArrivalCost(
    int boardCost,
    int alightSlack,
    int transitTime,
    T trip,
    int toStop
  ) {
    return delegate.transitArrivalCost(boardCost, alightSlack, transitTime, trip, toStop);
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

  private static void setWheelchairCost(
    int[] costIndex,
    WheelchairAccessibility wheelchair,
    WheelchairAccessibilityRequest requirements
  ) {
    costIndex[wheelchair.ordinal()] =
      switch (wheelchair) {
        case POSSIBLE -> 0;
        case NO_INFORMATION -> RaptorCostConverter.toRaptorCost(requirements.trips().unknownCost());
        case NOT_POSSIBLE -> RaptorCostConverter.toRaptorCost(
          requirements.trips().inaccessibleCost()
        );
      };
  }
}
