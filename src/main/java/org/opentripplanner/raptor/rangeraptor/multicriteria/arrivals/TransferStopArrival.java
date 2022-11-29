package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.raptor.api.view.TransferPathView;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.TransitArrival;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransferStopArrival<T extends RaptorTripSchedule>
  extends AbstractStopArrival<T> {

  private final RaptorTransfer transfer;

  public TransferStopArrival(
    AbstractStopArrival<T> previousState,
    RaptorTransfer transferPath,
    int arrivalTime
  ) {
    super(
      previousState,
      1,
      transferPath.stop(),
      arrivalTime,
      previousState.cost() + transferPath.generalizedCost()
    );
    this.transfer = transferPath;
  }

  @Override
  public TransitArrival<T> mostRecentTransitArrival() {
    return previous().mostRecentTransitArrival();
  }

  @Override
  public boolean arrivedByTransfer() {
    return true;
  }

  @Override
  public TransferPathView transferPath() {
    return () -> transfer;
  }
}
