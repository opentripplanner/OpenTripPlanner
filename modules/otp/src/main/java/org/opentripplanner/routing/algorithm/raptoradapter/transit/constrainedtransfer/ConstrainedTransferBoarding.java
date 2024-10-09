package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import java.util.function.Consumer;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;

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
    RaptorTransferConstraint constraint,
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
  public int tripIndex() {
    return tripIndex;
  }

  @Override
  public T trip() {
    return trip;
  }

  @Override
  public int stopPositionInPattern() {
    return stopPositionInPattern;
  }

  @Override
  public int time() {
    return time;
  }

  @Override
  public int earliestBoardTime() {
    return earliestBoardTime;
  }

  @Override
  public RaptorTransferConstraint transferConstraint() {
    return constraint;
  }

  @Override
  public boolean empty() {
    return false;
  }

  @Override
  public void boardWithFallback(
    Consumer<RaptorBoardOrAlightEvent<T>> boardCallback,
    Consumer<RaptorBoardOrAlightEvent<T>> alternativeBoardingFallback
  ) {
    if (empty()) {
      alternativeBoardingFallback.accept(this);
    } else if (!constraint.isNotAllowed()) {
      boardCallback.accept(this);
    }
  }
}
