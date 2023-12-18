package org.opentripplanner.raptor.api.path;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * Represent a transfer leg in a path.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransferPathLeg<T extends RaptorTripSchedule> implements PathLeg<T> {

  private final int fromStop;
  private final int fromTime;
  private final int toStop;
  private final int toTime;
  private final int c1;
  private final RaptorTransfer transfer;
  private final PathLeg<T> next;

  public TransferPathLeg(
    int fromStop,
    int fromTime,
    int toTime,
    int c1,
    RaptorTransfer transfer,
    PathLeg<T> next
  ) {
    this.fromStop = fromStop;
    this.fromTime = fromTime;
    this.toStop = transfer.stop();
    this.toTime = toTime;
    this.c1 = c1;
    this.transfer = transfer;
    this.next = next;
  }

  public RaptorTransfer transfer() {
    return transfer;
  }

  @Override
  public int fromTime() {
    return fromTime;
  }

  @Override
  public int fromStop() {
    return fromStop;
  }

  @Override
  public int toTime() {
    return toTime;
  }

  @Override
  public int toStop() {
    return toStop;
  }

  @Override
  public int c1() {
    return c1;
  }

  @Override
  public boolean isTransferLeg() {
    return true;
  }

  @Override
  public PathLeg<T> nextLeg() {
    return next;
  }

  @Override
  public int hashCode() {
    return Objects.hash(toStop, toTime, fromStop, fromTime, next);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransferPathLeg<?> that = (TransferPathLeg<?>) o;
    return (
      toStop == that.toStop &&
      toTime == that.toTime &&
      fromStop == that.fromStop &&
      fromTime == that.fromTime &&
      Objects.equals(next, that.next)
    );
  }

  @Override
  public String toString() {
    return "Walk " + asString(toStop());
  }
}
