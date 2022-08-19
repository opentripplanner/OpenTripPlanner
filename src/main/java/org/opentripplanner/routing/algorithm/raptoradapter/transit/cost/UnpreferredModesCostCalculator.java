package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.Set;
import java.util.function.DoubleFunction;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;

/**
 * Cost calculator for {@link TransitMode}(s) that have been configured to be unpreferred.
 */
public class UnpreferredModesCostCalculator<T extends DefaultTripSchedule>
  implements CostCalculator<T> {

  private CostCalculator<T> delegate;
  private Set<TransitMode> unpreferredModes;
  private DoubleFunction<Double> unpreferredModesCost;

  public UnpreferredModesCostCalculator(
    @Nonnull CostCalculator<T> delegate,
    Set<TransitMode> unpreferredModes,
    DoubleFunction<Double> unpreferredModesCost
  ) {
    this.delegate = delegate;
    this.unpreferredModes = unpreferredModes;
    this.unpreferredModesCost = unpreferredModesCost;
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
    var defaultCost = delegate.transitArrivalCost(
      boardCost,
      alightSlack,
      transitTime,
      trip,
      toStop
    );
    var mode = trip.transitMode();
    if (unpreferredModes.contains(mode)) {
      return (
        defaultCost + RaptorCostConverter.toRaptorCost(unpreferredModesCost.apply(transitTime))
      );
    }
    return defaultCost;
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
