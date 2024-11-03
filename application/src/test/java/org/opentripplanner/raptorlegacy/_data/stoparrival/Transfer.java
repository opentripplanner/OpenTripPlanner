package org.opentripplanner.raptorlegacy._data.stoparrival;

import static org.opentripplanner.raptor.api.model.PathLegType.TRANSFER;

import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;

/*
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
class Transfer extends AbstractStopArrival {

  private final RaptorTransfer transfer;

  Transfer(
    int round,
    int arrivalTime,
    RaptorTransfer transfer,
    ArrivalView<TestTripSchedule> previous
  ) {
    super(round, transfer.stop(), arrivalTime, transfer.c1(), previous.c2(), previous);
    this.transfer = transfer;
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
