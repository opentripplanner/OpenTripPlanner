package org.opentripplanner.raptor.api.path;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * Represent a transit leg in a path.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransitPathLeg<T extends RaptorTripSchedule> implements PathLeg<T> {

  private final T trip;
  private final int boardTime;
  private final int alightTime;
  private final int boardStopPos;
  private final int alightStopPos;
  private final RaptorConstrainedTransfer constrainedTransferAfterLeg;
  private final int c1;
  private final PathLeg<T> next;
  private final int boardStop;
  private final int alightStop;

  public TransitPathLeg(
    T trip,
    int boardTime,
    int alightTime,
    int boardStopPos,
    int alightStopPos,
    RaptorConstrainedTransfer constrainedTransferAfterLeg,
    int c1,
    PathLeg<T> next
  ) {
    this.trip = trip;
    this.boardTime = boardTime;
    this.alightTime = alightTime;
    this.boardStopPos = boardStopPos;
    this.alightStopPos = alightStopPos;
    this.constrainedTransferAfterLeg = constrainedTransferAfterLeg;
    this.c1 = c1;
    this.next = next;
    this.boardStop = trip.pattern().stopIndex(boardStopPos);
    this.alightStop = trip.pattern().stopIndex(alightStopPos);
  }

  /**
   * The trip schedule info object passed into Raptor routing algorithm.
   */
  public T trip() {
    return trip;
  }

  public int getFromStopPosition() {
    return boardStopPos;
  }

  public int getToStopPosition() {
    return alightStopPos;
  }

  public RaptorConstrainedTransfer getConstrainedTransferAfterLeg() {
    return constrainedTransferAfterLeg;
  }

  @Override
  public int fromTime() {
    return boardTime;
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
    return alightTime;
  }

  /**
   * The stop index where the leg ends, also called arrival stop index.
   */
  @Override
  public int toStop() {
    return alightStop;
  }

  @Override
  public int c1() {
    return c1;
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
    return Objects.hash(boardTime, boardStopPos, alightTime, alightStopPos, trip, next);
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
      boardTime == that.boardTime &&
      boardStopPos == that.boardStopPos &&
      alightTime == that.alightTime &&
      alightStopPos == that.alightStopPos &&
      trip.equals(that.trip) &&
      next.equals(that.next)
    );
  }

  @Override
  public String toString() {
    return trip.pattern().debugInfo() + " " + asString(toStop());
  }
}
