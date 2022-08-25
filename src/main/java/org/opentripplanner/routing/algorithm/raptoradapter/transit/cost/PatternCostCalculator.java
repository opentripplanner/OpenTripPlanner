package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.BitSet;
import java.util.function.DoubleFunction;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;

public class PatternCostCalculator<T extends DefaultTripSchedule> implements CostCalculator<T> {

  public static final double DEFAULT_ROUTE_RELUCTANCE = 1.0;
  public static final double UNPREFERRED_ROUTE_RELUCTANCE = 2.0;

  private final CostCalculator<T> delegate;
  private final BitSet unpreferredPatterns;
  private final DoubleFunction<Double> unpreferredCost;

  public PatternCostCalculator(
    @Nonnull CostCalculator<T> delegate,
    @Nonnull BitSet unpreferredPatterns,
    @Nonnull DoubleFunction<Double> unpreferredCost
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
    int unpreferCost = 0;
    if (unpreferredPatterns.get(trip.pattern().patternIndex())) {
      // calculate cost with linear function: fixed + reluctance * transitTime
      unpreferCost += RaptorCostConverter.toRaptorCost(unpreferredCost.apply(transitTime));
    }
    int defaultCost = delegate.transitArrivalCost(
      boardCost,
      alightSlack,
      transitTime,
      trip,
      toStop
    );
    return defaultCost + unpreferCost;
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
