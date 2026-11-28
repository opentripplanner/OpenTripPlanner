package org.opentripplanner.raptor.api.path;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

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
  private final int durationInSeconds;
  private final RaptorTransfer viaTransfer;
  private final PathLeg<T> next;

  /**
   * @param toStop the stop this transfer leg arrives at, travelling in the direction from origin to
   *               destination. This must be passed in rather than derived from
   *               {@code transfer.stop()}: in a reverse search, a transfer obtained through
   *               {@code RaptorTransitDataProvider#getTransfersToStop(int)} reports {@code stop()}
   *               as the leg's origin, not its destination (see
   *               {@link org.opentripplanner.raptor.path.PathBuilderLeg#transferLeg}).
   * @param durationInSeconds the time it takes to travel the transfer, not including wait time.
   * @param viaTransfer the transfer passed in as part of a via connection, or {@code null} for a
   *                    regular transfer. A regular transfer is not part of the path; look it up
   *                    from the transfer data by {@link #fromStop()} and {@link #toStop()}. The via
   *                    transfer is request specific and can not be looked up.
   */
  public TransferPathLeg(
    int fromStop,
    int fromTime,
    int toStop,
    int toTime,
    int c1,
    int durationInSeconds,
    @Nullable RaptorTransfer viaTransfer,
    PathLeg<T> next
  ) {
    this.fromStop = fromStop;
    this.fromTime = fromTime;
    this.toStop = toStop;
    this.toTime = toTime;
    this.c1 = c1;
    this.durationInSeconds = durationInSeconds;
    this.viaTransfer = viaTransfer;
    this.next = next;
  }

  public int durationInSeconds() {
    return durationInSeconds;
  }

  /**
   * The transfer of a via connection, or {@code null} if this is a regular transfer. See the
   * constructor.
   */
  @Nullable
  public RaptorTransfer viaTransfer() {
    return viaTransfer;
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
