package org.opentripplanner.ext.sorlandsbanen;

import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;
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
  public int transitCost(int transitDuration, T tripScheduledBoarded) {
    return delegate.transitCost(transitDuration, tripScheduledBoarded);
  }

  @Override
  public int transitArrivalCost(
    int boardCost,
    int alightSlack,
    int transitDuration,
    T trip,
    int toStopIndex
  ) {
    int cost = delegate.transitArrivalCost(
      boardCost,
      alightSlack,
      transitDuration,
      trip,
      toStopIndex
    );

    // This is a bit ugly, since it relies on the fact that the 'transitReluctanceFactorIndex'
    // returns the 'route.getMode().ordinal()'
    if (trip.transitReluctanceFactorIndex() == TransitMode.COACH.ordinal()) {
      cost += transitDuration * EXTRA_RELUCTANCE_ON_COACH;
    }
    return cost;
  }

  @Override
  public int waitCost(int waitTimeInSeconds) {
    return delegate.waitCost(waitTimeInSeconds);
  }

  @Override
  public int calculateRemainingMinCost(
    int minTravelDuration,
    int minNumTransfers,
    int fromStopIndex
  ) {
    return delegate.calculateRemainingMinCost(minTravelDuration, minNumTransfers, fromStopIndex);
  }

  @Override
  public int costEgress(int stopIndex, boolean egressHasRides) {
    return delegate.costEgress(stopIndex, egressHasRides);
  }
}
