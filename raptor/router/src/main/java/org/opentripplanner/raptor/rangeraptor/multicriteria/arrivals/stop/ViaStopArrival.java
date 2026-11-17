package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * A transfer arrival which is part of a via visit. A regular transfer arrival is not, see
 * {@link TransferStopArrival}. The arrival is one of:
 * <ul>
 *   <li>The transfer of a via connection. The via transfer is request specific and can not be
 *       looked up when the path is created, so it is kept in the state, see
 *       {@link #viaTransfer()}. The time between the previous arrival and this arrival includes
 *       the minimum wait time of the via connection, so the duration is taken from the
 *       transfer.</li>
 *   <li>A regular transfer where a via visit has added a wait time to the arrival time, see
 *       {@link #viaWaitTime()}. The wait time is the sum, if the arrival is a visit of more than
 *       one via location. The via transfer is {@code null}.</li>
 * </ul>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class ViaStopArrival<T extends RaptorTripSchedule> extends TransferStopArrival<T> {

  @Nullable
  private final RaptorTransfer viaTransfer;

  private final int viaWaitTime;

  private ViaStopArrival(
    McStopArrival<T> previous,
    int toStop,
    int arrivalTime,
    int transferC1,
    @Nullable RaptorTransfer viaTransfer,
    int viaWaitTime
  ) {
    super(previous, toStop, arrivalTime, transferC1);
    this.viaTransfer = viaTransfer;
    this.viaWaitTime = viaWaitTime;
  }

  /** Create the arrival of the transfer of a via connection. */
  static <T extends RaptorTripSchedule> ViaStopArrival<T> ofViaTransfer(
    McStopArrival<T> previous,
    RaptorTransfer viaTransfer,
    int arrivalTime
  ) {
    return new ViaStopArrival<>(
      previous,
      viaTransfer.stop(),
      arrivalTime,
      viaTransfer.c1(),
      viaTransfer,
      0
    );
  }

  /** Create the arrival of a regular transfer, with a via visit wait time added to it. */
  static <T extends RaptorTripSchedule> ViaStopArrival<T> ofRegularTransfer(
    TransferStopArrival<T> transfer,
    int viaWaitTime
  ) {
    return new ViaStopArrival<>(
      transfer.previous(),
      transfer.stop(),
      transfer.arrivalTime() + viaWaitTime,
      transfer.transferC1(),
      null,
      viaWaitTime
    );
  }

  @Override
  @Nullable
  public RaptorTransfer viaTransfer() {
    return viaTransfer;
  }

  @Override
  public int viaWaitTime() {
    return viaWaitTime;
  }

  @Override
  public McStopArrival<T> addSlackToArrivalTime(int slack) {
    return new ViaStopArrival<>(
      previous(),
      stop(),
      arrivalTime() + slack,
      transferC1(),
      viaTransfer,
      viaWaitTime + slack
    );
  }
}
