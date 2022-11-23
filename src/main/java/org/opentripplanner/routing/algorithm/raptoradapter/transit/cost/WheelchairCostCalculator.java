package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import javax.annotation.Nonnull;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.transit.model.basic.Accessibility;

public class WheelchairCostCalculator<T extends DefaultTripSchedule> implements CostCalculator<T> {

  private final CostCalculator<T> delegate;
  private final int[] wheelchairBoardingCost;

  public WheelchairCostCalculator(
    @Nonnull CostCalculator<T> delegate,
    @Nonnull AccessibilityPreferences wheelchairAccessibility
  ) {
    this.delegate = delegate;
    this.wheelchairBoardingCost = createWheelchairCost(wheelchairAccessibility);
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
  public int costEgress(RaptorAccessEgress egress) {
    return delegate.costEgress(egress);
  }

  /**
   * Create the wheelchair costs for boarding a trip with all possible accessibility values
   */
  private static int[] createWheelchairCost(AccessibilityPreferences requirements) {
    int[] costIndex = new int[Accessibility.values().length];

    for (var it : Accessibility.values()) {
      costIndex[it.ordinal()] =
        switch (it) {
          case POSSIBLE -> CostCalculator.ZERO_COST;
          case NO_INFORMATION -> RaptorCostConverter.toRaptorCost(requirements.unknownCost());
          case NOT_POSSIBLE -> RaptorCostConverter.toRaptorCost(requirements.inaccessibleCost());
        };
    }
    return costIndex;
  }
}
