package org.opentripplanner.raptor._data.stoparrival;

import static org.opentripplanner.raptor.api.model.PathLegType.TRANSFER;

import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.view.ArrivalView;

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
