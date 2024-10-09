package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c2;

import static org.opentripplanner.raptor.api.model.PathLegType.TRANSFER;

import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.TransitArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class TransferStopArrivalC2<T extends RaptorTripSchedule> extends AbstractStopArrivalC2<T> {

  private final RaptorTransfer transfer;

  TransferStopArrivalC2(McStopArrival<T> previous, RaptorTransfer transferPath, int arrivalTime) {
    super(
      previous,
      1,
      transferPath.stop(),
      arrivalTime,
      previous.c1() + transferPath.c1(),
      previous.c2()
    );
    this.transfer = transferPath;
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
  public RaptorTransfer transfer() {
    return transfer;
  }

  @Override
  public boolean arrivedOnBoard() {
    return false;
  }
}
