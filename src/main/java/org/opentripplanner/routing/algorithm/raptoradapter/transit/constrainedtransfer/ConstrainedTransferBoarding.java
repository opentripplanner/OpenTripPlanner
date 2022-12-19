package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * A boarding event passed to Raptor to perform a boarding.
 */
public class ConstrainedTransferBoarding<T extends RaptorTripSchedule>
  implements RaptorBoardOrAlightEvent<T> {

  private final RaptorTransferConstraint constraint;
  private final int tripIndex;
  private final T trip;
  private final int stopPositionInPattern;
  private final int time;
  private final int earliestBoardTime;

  public ConstrainedTransferBoarding(
    @Nonnull RaptorTransferConstraint constraint,
    int tripIndex,
    T trip,
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
  public int getEarliestBoardTime() {
    return earliestBoardTime;
  }

  @Override
  @Nonnull
  public RaptorTransferConstraint getTransferConstraint() {
    return constraint;
  }

  @Override
  public boolean empty() {
    return false;
  }

  @Override
  public void boardWithFallback(
    Consumer<RaptorBoardOrAlightEvent<T>> boardCallback,
    Consumer<RaptorBoardOrAlightEvent<T>> fallback
  ) {
    if (empty()) {
      fallback.accept(this);
    } else if (!constraint.isNotAllowed()) {
      boardCallback.accept(this);
    }
  }
}
