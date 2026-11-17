package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop;

import static org.opentripplanner.raptor.api.view.PathLegType.TRANSFER;

import org.opentripplanner.raptor.api.view.PathLegType;
import org.opentripplanner.raptor.api.view.TransitArrival;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
sealed class TransferStopArrivalC2<T extends RaptorTripSchedule>
  extends AbstractStopArrivalC2<T>
  permits ViaTransferStopArrivalC2
{

  TransferStopArrivalC2(McStopArrival<T> previous, int toStop, int arrivalTime, int transferC1) {
    super(
      previous,
      previous.round(),
      toStop,
      arrivalTime,
      previous.c1() + transferC1,
      previous.c2()
    );
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
    return new TransferStopArrivalC2<>(previous(), stop(), arrivalTime() + slack, transferC1());
  }
}
