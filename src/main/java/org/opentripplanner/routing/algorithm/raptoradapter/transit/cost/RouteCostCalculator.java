package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;

public class RouteCostCalculator<T extends DefaultTripSchedule> implements CostCalculator<T> {

  private final CostCalculator delegate;
  private final Map<FeedScopedId, Integer> routePenalties;

  public RouteCostCalculator(
    @Nonnull CostCalculator<T> delegate,
    @Nonnull Map<FeedScopedId, Integer> routePenalties
  ) {
    this.routePenalties = routePenalties;
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
    int routeReluctanceCost = routePenalties.getOrDefault(trip.routeId(), ZERO_COST);
    return defaultCost + routeReluctanceCost;
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
