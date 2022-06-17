package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import javax.annotation.Nonnull;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;
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
    this.delegate = delegate;
    this.wheelchairBoardingCost = createWheelchairCost(requirements);
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

  /**
   * Create the wheelchair costs for boarding a trip with all possible accessibility values
   */
  private static int[] createWheelchairCost(WheelchairAccessibilityRequest requirements) {
    int[] costIndex = new int[WheelchairAccessibility.values().length];

    for (var it : WheelchairAccessibility.values()) {
      costIndex[it.ordinal()] =
        switch (it) {
          case POSSIBLE -> 0;
          case NO_INFORMATION -> RaptorCostConverter.toRaptorCost(
            requirements.trip().unknownCost()
          );
          case NOT_POSSIBLE -> RaptorCostConverter.toRaptorCost(
            requirements.trip().inaccessibleCost()
          );
        };
    }
    return costIndex;
  }
}
