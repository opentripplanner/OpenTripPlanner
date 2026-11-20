package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop;

import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * A transfer arrival created by the transfer of a via connection. The via transfer is request
 * specific and can not be looked up when the path is created, so it is kept in the state. A regular
 * transfer is not, see {@link TransferStopArrival}.
 * <p>
 * The time between the previous arrival and this arrival includes the minimum wait time of the via
 * connection, so the duration is taken from the transfer.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class ViaTransferStopArrival<T extends RaptorTripSchedule> extends TransferStopArrival<T> {

  private final RaptorTransfer viaTransfer;

  ViaTransferStopArrival(McStopArrival<T> previous, RaptorTransfer viaTransfer, int arrivalTime) {
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
    return new ViaTransferStopArrival<>(previous(), viaTransfer, arrivalTime() + slack);
  }
}
