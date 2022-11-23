package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view;

import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.TransferPathView;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.StopArrivalState;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

final class Transfer<T extends RaptorTripSchedule>
  extends StopArrivalViewAdapter<T>
  implements TransferPathView {

  private final StopArrivalState<T> arrival;
  private final StopsCursor<T> cursor;

  Transfer(int round, int stop, StopArrivalState<T> arrival, StopsCursor<T> cursor) {
    super(round, stop);
    this.arrival = arrival;
    this.cursor = cursor;
  }

  @Override
  public int arrivalTime() {
    return arrival.time();
  }

  @Override
  public ArrivalView<T> previous() {
    return cursor.stop(round(), arrival.transferFromStop(), true);
  }

  @Override
  public boolean arrivedByTransfer() {
    return true;
  }

  @Override
  public TransferPathView transferPath() {
    return this;
  }

  @Override
  public RaptorTransfer transfer() {
    return arrival.transferPath();
  }

  @Override
  public int durationInSeconds() {
    return arrival.transferPath().durationInSeconds();
  }
}
