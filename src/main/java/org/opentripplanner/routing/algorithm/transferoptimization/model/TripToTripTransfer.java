package org.opentripplanner.routing.algorithm.transferoptimization.model;

import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public class TripToTripTransfer<T extends RaptorTripSchedule> {

  private final TripStopTime<T> from;
  private final TripStopTime<T> to;
  private final RaptorTransfer transfer;

  public TripToTripTransfer(
      TripStopTime<T> from,
      TripStopTime<T> to,
      RaptorTransfer transfer
  ) {
    this.from = from;
    this.to = to;
    this.transfer = transfer;
  }

  public TripStopTime<T> from() {
    return from;
  }

  public TripStopTime<T> to() {
    return to;
  }

  public int transferDuration() {
    return sameStop() ? 0 : transfer.durationInSeconds();
  }

  public boolean sameStop() {
    return from.stop() == to.stop();
  }

  @Nullable
  public RaptorTransfer getTransfer() {
    return transfer;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripToTripTransfer.class)
            .addObj("from", from)
            .addObj("to", to)
            .addObj("transfer", transfer)
            .toString();
  }
}
