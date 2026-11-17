package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view;

import static org.opentripplanner.raptor.api.view.PathLegType.TRANSFER;

import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.PathLegType;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.StopArrivalState;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

final class Transfer<T extends RaptorTripSchedule> extends StopArrivalViewAdapter<T> {

  private final int arrivalTime;
  private final RaptorTransfer transfer;
  private final ArrivalView<T> previous;

  /**
   * The values are copied from the mutable state, so the arrival and its chain stay valid after
   * the state is overwritten by the following iterations.
   */
  Transfer(int round, int stop, StopArrivalState<T> arrival, StopsCursor<T> cursor) {
    super(round, stop);
    this.arrivalTime = arrival.time();
    this.transfer = arrival.transferPath();
    this.previous = cursor.stop(round(), arrival.transferFromStop(), true);
  }

  @Override
  public int c1() {
    return RaptorCostCalculator.ZERO_COST;
  }

  @Override
  public int c2() {
    return RaptorConstants.NOT_SET;
  }

  @Override
  public int arrivalTime() {
    return arrivalTime;
  }

  @Override
  public ArrivalView<T> previous() {
    return previous;
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
