package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.BitSet;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;

class PatternCostCalculator<T extends DefaultTripSchedule> implements RaptorCostCalculator<T> {

  private final RaptorCostCalculator<T> delegate;
  private final BitSet unpreferredPatterns;
  private final RaptorCostLinearFunction unpreferredCost;

  PatternCostCalculator(
    RaptorCostCalculator<T> delegate,
    BitSet unpreferredPatterns,
    RaptorCostLinearFunction unpreferredCost
  ) {
    this.unpreferredPatterns = unpreferredPatterns;
    this.delegate = delegate;
    this.unpreferredCost = unpreferredCost;
  }

  @Override
  public int boardingCost(
    boolean firstBoarding,
    int prevArrivalTime,
    int boardStopIndex,
    int boardTime,
    T trip,
    RaptorTransferConstraint transferConstraints
  ) {
    return delegate.boardingCost(
      firstBoarding,
      prevArrivalTime,
      boardStopIndex,
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
    int toStopIndex
  ) {
    int defaultCost = delegate.transitArrivalCost(
      boardCost,
      alightSlack,
      transitTime,
      trip,
      toStopIndex
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
  public int calculateRemainingMinCost(int minTravelTime, int minNumTransfers, int fromStopIndex) {
    return delegate.calculateRemainingMinCost(minTravelTime, minNumTransfers, fromStopIndex);
  }

  @Override
  public int costEgress(int stopIndex, boolean egressHasRides) {
    return delegate.costEgress(stopIndex, egressHasRides);
  }
}
