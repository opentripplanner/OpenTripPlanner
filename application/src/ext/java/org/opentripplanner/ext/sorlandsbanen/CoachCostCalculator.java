package org.opentripplanner.ext.sorlandsbanen;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * This cost calculator increases the cost on mode coach by adding an extra reluctance. The
 * reluctance is hardcoded in this class and cannot be configured.
 */
class CoachCostCalculator<T extends TripSchedule> implements RaptorCostCalculator<T> {

  private static final int EXTRA_RELUCTANCE_ON_COACH = RaptorCostConverter.toRaptorCost(0.6);

  private final RaptorCostCalculator<T> delegate;

  CoachCostCalculator(RaptorCostCalculator<T> delegate) {
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
    int cost = delegate.transitArrivalCost(boardCost, alightSlack, transitTime, trip, toStop);

    // This is a bit ugly, since it relies on the fact that the 'transitReluctanceFactorIndex'
    // returns the 'route.getMode().ordinal()'
    if (trip.transitReluctanceFactorIndex() == TransitMode.COACH.ordinal()) {
      cost += transitTime * EXTRA_RELUCTANCE_ON_COACH;
    }
    return cost;
  }

  @Override
  public int waitCost(int waitTimeInSeconds) {
    return delegate.waitCost(waitTimeInSeconds);
  }

  @Override
  public int calculateRemainingMinCost(int minTravelTime, int minNumTransfers, int fromStop) {
    return delegate.calculateRemainingMinCost(minTravelTime, minNumTransfers, fromStop);
  }

  @Override
  public int costEgress(RaptorAccessEgress egress) {
    return delegate.costEgress(egress);
  }
}
