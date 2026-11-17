package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop;

import static org.opentripplanner.raptor.api.view.PathLegType.TRANSFER;

import org.opentripplanner.raptor.api.view.PathLegType;
import org.opentripplanner.raptor.api.view.TransitArrival;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
sealed class TransferStopArrival<T extends RaptorTripSchedule>
  extends McStopArrival<T>
  permits ViaTransferStopArrival
{

  /**
   * The transfer is not stored, only the target stop and the cost. The duration is the time between
   * the previous arrival and this.
   */
  TransferStopArrival(McStopArrival<T> previousState, int toStop, int arrivalTime, int transferC1) {
    super(
      previousState,
      // Transfer happens in the same round as the previous transit
      previousState.round(),
      toStop,
      arrivalTime,
      previousState.c1() + transferC1
    );
  }

  @Override
  public int c2() {
    return RaptorConstants.NOT_SET;
  }

  @Override
  public TransitArrival<T> mostRecentTransitArrival() {
    return previous().mostRecentTransitArrival();
  }

  @Override
  public PathLegType arrivedBy() {
    return TRANSFER;
  }

  @Override
  public boolean arrivedOnBoard() {
    return false;
  }

  @Override
  public McStopArrival<T> addSlackToArrivalTime(int slack) {
    return new TransferStopArrival<>(previous(), stop(), arrivalTime() + slack, transferC1());
  }
}
