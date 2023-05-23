package org.opentripplanner.raptor._data.stoparrival;

import static org.opentripplanner.raptor.api.model.PathLegType.TRANSFER;

import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.view.ArrivalView;

public class Transfer extends AbstractStopArrival {

  private final RaptorTransfer transfer;

  public Transfer(
    int round,
    int stop,
    int departureTime,
    int arrivalTime,
    int extraCost,
    ArrivalView<TestTripSchedule> previous
  ) {
    super(round, stop, arrivalTime, extraCost, previous);
    // In a reverse search we the arrival is before the departure
    int durationInSeconds = Math.abs(arrivalTime - departureTime);
    this.transfer = TestTransfer.transfer(stop, durationInSeconds, extraCost);
  }

  public Transfer(
    int round,
    int arrivalTime,
    RaptorTransfer transfer,
    ArrivalView<TestTripSchedule> previous
  ) {
    super(round, transfer.stop(), arrivalTime, transfer.generalizedCost(), previous);
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
