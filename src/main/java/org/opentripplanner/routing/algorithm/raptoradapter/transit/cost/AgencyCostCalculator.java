package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.Set;
import java.util.function.DoubleFunction;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;

public class AgencyCostCalculator<T extends DefaultTripSchedule> extends IdBasedCostCalculator<T> {

  public static final double DEFAULT_AGENCY_RELUCTANCE = 1.0;
  public static final double UNPREFERRED_AGENCY_RELUCTANCE = 2.0;

  public AgencyCostCalculator(
    @Nonnull CostCalculator<T> delegate,
    @Nonnull Set<FeedScopedId> unpreferredIds,
    @Nonnull DoubleFunction<Double> unpreferredCost
  ) {
    super(delegate, unpreferredIds, unpreferredCost, DefaultTripSchedule::agencyId);
  }
}
