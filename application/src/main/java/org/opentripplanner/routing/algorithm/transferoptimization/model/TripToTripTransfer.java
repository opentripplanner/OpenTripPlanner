package org.opentripplanner.routing.algorithm.transferoptimization.model;

import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class TripToTripTransfer<T extends RaptorTripSchedule> {

  private final TripStopTime<T> from;
  private final TripStopTime<T> to;
  private final RaptorTransfer pathTransfer;
  private final ConstrainedTransfer constrainedTransfer;

  public TripToTripTransfer(
    TripStopTime<T> from,
    TripStopTime<T> to,
    RaptorTransfer pathTransfer,
    @Nullable ConstrainedTransfer constrainedTransfer
  ) {
    this.from = from;
    this.to = to;
    this.pathTransfer = pathTransfer;
    this.constrainedTransfer = constrainedTransfer;
  }

  public TripStopTime<T> from() {
    return from;
  }

  public TripStopTime<T> to() {
    return to;
  }

  /**
   * The time it takes to transfer between the given from and to stop. For a transfer at the same
   * stop the time is zero.
   */
  public int transferDuration() {
    return sameStop() ? 0 : pathTransfer.durationInSeconds();
  }

  public int generalizedCost() {
    return sameStop() ? 0 : pathTransfer.c1();
  }

  public boolean sameStop() {
    return from.stop() == to.stop();
  }

  @Nullable
  public RaptorTransfer getPathTransfer() {
    return pathTransfer;
  }

  @Nullable
  public ConstrainedTransfer constrainedTransfer() {
    return constrainedTransfer;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripToTripTransfer.class)
      .addObj("from", from)
      .addObj("to", to)
      .addObj("transfer", pathTransfer)
      .toString();
  }
}
