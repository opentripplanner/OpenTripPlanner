package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import javax.annotation.Nonnull;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleBoardOrAlightEvent;

/**
 * A boarding event passed to Raptor to perform a boarding.
 */
public class ConstrainedTransferBoarding<T extends RaptorTripSchedule>
  implements RaptorTripScheduleBoardOrAlightEvent<T> {

  private final RaptorTransferConstraint constraint;
  private final int tripIndex;
  private final T trip;
  private final int stopPositionInPattern;
  private final int time;
  private final int earliestBoardTime;

  public ConstrainedTransferBoarding(
    @Nonnull RaptorTransferConstraint constraint,
    int tripIndex,
    @Nonnull T trip,
    int stopPositionInPattern,
    int time,
    int earliestBoardTime
  ) {
    this.constraint = constraint;
    this.tripIndex = tripIndex;
    this.trip = trip;
    this.stopPositionInPattern = stopPositionInPattern;
    this.time = time;
    this.earliestBoardTime = earliestBoardTime;
  }

  @Override
  public int getTripIndex() {
    return tripIndex;
  }

  @Override
  @Nonnull
  public T getTrip() {
    return trip;
  }

  @Override
  public int getStopPositionInPattern() {
    return stopPositionInPattern;
  }

  @Override
  public int getTime() {
    return time;
  }

  @Override
  public int getEarliestBoardTimeForConstrainedTransfer() {
    return earliestBoardTime;
  }

  @Override
  @Nonnull
  public RaptorTransferConstraint getTransferConstraint() {
    return constraint;
  }
}
