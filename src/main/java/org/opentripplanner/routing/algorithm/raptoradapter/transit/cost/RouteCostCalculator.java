package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.Set;
import java.util.function.DoubleFunction;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;

public class RouteCostCalculator<T extends DefaultTripSchedule> implements CostCalculator<T> {

  public static final double DEFAULT_ROUTE_RELUCTANCE = 1.0;
  public static final double UNPREFERRED_ROUTE_RELUCTANCE = 2.0;

  private final CostCalculator delegate;
  private final Set<FeedScopedId> unpreferredRoutes;
  private final DoubleFunction<Double> unpreferredRouteCost;

  public RouteCostCalculator(
    @Nonnull CostCalculator<T> delegate,
    @Nonnull Set<FeedScopedId> routePenalties,
    @Nonnull DoubleFunction<Double> unpreferredRouteCost
  ) {
    this.unpreferredRoutes = routePenalties;
    this.delegate = delegate;
    this.unpreferredRouteCost = unpreferredRouteCost;
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
    if (unpreferredRoutes.contains(trip.routeId())) {
      // calculate cost with linear function: fixed + reluctance * transitTime
      unpreferCost += RaptorCostConverter.toRaptorCost(unpreferredRouteCost.apply(transitTime));
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
