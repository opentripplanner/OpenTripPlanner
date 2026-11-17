package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop;

import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * The C2 version of {@link ViaTransferStopArrival}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class ViaTransferStopArrivalC2<T extends RaptorTripSchedule> extends TransferStopArrivalC2<T> {

  private final RaptorTransfer viaTransfer;

  ViaTransferStopArrivalC2(McStopArrival<T> previous, RaptorTransfer viaTransfer, int arrivalTime) {
    super(previous, viaTransfer.stop(), arrivalTime, viaTransfer.c1());
    this.viaTransfer = viaTransfer;
  }

  @Override
  public int transferDuration() {
    return viaTransfer.durationInSeconds();
  }

  @Override
  public RaptorTransfer viaTransfer() {
    return viaTransfer;
  }

  @Override
  public McStopArrival<T> addSlackToArrivalTime(int slack) {
    return new ViaTransferStopArrivalC2<>(previous(), viaTransfer, arrivalTime() + slack);
  }
}
