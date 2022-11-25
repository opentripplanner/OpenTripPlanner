package org.opentripplanner.raptor.api.path;

import java.util.Objects;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;
import org.opentripplanner.raptor.spi.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Represent a transit leg in a path.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransitPathLeg<T extends RaptorTripSchedule> implements PathLeg<T> {

  private final T trip;
  private final BoardAndAlightTime boardAndAlightTime;
  private final RaptorConstrainedTransfer constrainedTransferAfterLeg;
  private final int cost;
  private final PathLeg<T> next;
  private final int boardStop;
  private final int alightStop;

  public TransitPathLeg(
    T trip,
    BoardAndAlightTime boardAndAlightTime,
    RaptorConstrainedTransfer constrainedTransferAfterLeg,
    int cost,
    PathLeg<T> next
  ) {
    this.trip = trip;
    this.boardAndAlightTime = boardAndAlightTime;
    this.constrainedTransferAfterLeg = constrainedTransferAfterLeg;
    this.cost = cost;
    this.next = next;
    this.boardStop = trip.pattern().stopIndex(boardAndAlightTime.boardStopPos());
    this.alightStop = trip.pattern().stopIndex(boardAndAlightTime.alightStopPos());
  }

  /**
   * The trip schedule info object passed into Raptor routing algorithm.
   */
  public T trip() {
    return trip;
  }

  public int getFromStopPosition() {
    return boardAndAlightTime.boardStopPos();
  }

  public int getToStopPosition() {
    return boardAndAlightTime.alightStopPos();
  }

  public RaptorConstrainedTransfer getConstrainedTransferAfterLeg() {
    return constrainedTransferAfterLeg;
  }

  @Override
  public int fromTime() {
    return boardAndAlightTime.boardTime();
  }

  /**
   * The stop index where the leg starts. Also called departure stop index.
   */
  @Override
  public int fromStop() {
    return boardStop;
  }

  @Override
  public int toTime() {
    return boardAndAlightTime.alightTime();
  }

  /**
   * The stop index where the leg ends, also called arrival stop index.
   */
  @Override
  public int toStop() {
    return alightStop;
  }

  @Override
  public int generalizedCost() {
    return cost;
  }

  @Override
  public boolean isTransitLeg() {
    return true;
  }

  @Override
  public PathLeg<T> nextLeg() {
    return next;
  }

  public boolean isStaySeatedOntoNextLeg() {
    return (
      constrainedTransferAfterLeg != null &&
      constrainedTransferAfterLeg.getTransferConstraint().isStaySeated()
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(boardAndAlightTime, trip, next);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransitPathLeg<?> that = (TransitPathLeg<?>) o;
    return (
      boardAndAlightTime.equals(that.boardAndAlightTime) &&
      trip.equals(that.trip) &&
      next.equals(that.next)
    );
  }

  @Override
  public String toString() {
    return trip.pattern().debugInfo() + " " + asString(toStop());
  }
}
