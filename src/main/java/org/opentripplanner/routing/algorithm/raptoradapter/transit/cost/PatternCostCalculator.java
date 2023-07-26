package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.BitSet;
import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;

class PatternCostCalculator<T extends DefaultTripSchedule> implements RaptorCostCalculator<T> {

  private final RaptorCostCalculator<T> delegate;
  private final BitSet unpreferredPatterns;
  private final RaptorCostLinearFunction unpreferredCost;

  PatternCostCalculator(
    @Nonnull RaptorCostCalculator<T> delegate,
    @Nonnull BitSet unpreferredPatterns,
    @Nonnull RaptorCostLinearFunction unpreferredCost
  ) {
    this.unpreferredPatterns = unpreferredPatterns;
    this.delegate = delegate;
    this.unpreferredCost = unpreferredCost;
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
    return delegate.boardingCost(
      firstBoarding,
      prevArrivalTime,
      boardStop,
      boardTime,
      trip,
      transferConstraints
    );
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
    int defaultCost = delegate.transitArrivalCost(
      boardCost,
      alightSlack,
      transitTime,
      trip,
      toStop
    );
    boolean includeUnpreferredCost = unpreferredPatterns.get(trip.pattern().patternIndex());

    if (includeUnpreferredCost) {
      int unpreferredCostValue = unpreferredCost.calculateRaptorCost(transitTime);
      return defaultCost + unpreferredCostValue;
    } else {
      return defaultCost;
    }
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
}
