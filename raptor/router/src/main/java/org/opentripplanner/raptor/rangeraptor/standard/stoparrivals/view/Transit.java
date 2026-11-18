package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view;

import static org.opentripplanner.raptor.api.view.PathLegType.TRANSIT;

import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.PathLegType;
import org.opentripplanner.raptor.api.view.TransitPathView;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.StopArrivalState;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

final class Transit<T extends RaptorTripSchedule>
  extends StopArrivalViewAdapter<T>
  implements TransitPathView<T>
{

  private final int arrivalTime;
  private final int boardStopPosition;
  private final T trip;
  private final ArrivalView<T> previous;

  /**
   * The values are copied from the mutable state, so the arrival and its chain stay valid after
   * the state is overwritten by the following iterations.
   */
  Transit(int round, int stop, StopArrivalState<T> arrival, StopsCursor<T> cursor) {
    super(round, stop);
    this.arrivalTime = arrival.onBoardArrivalTime();
    this.boardStopPosition = arrival.boardStopPosition();
    this.trip = arrival.trip();
    this.previous = cursor.stop(round() - 1, boardStopIndex(), this);
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
    return TRANSIT;
  }

  @Override
  public TransitPathView<T> transitPath() {
    return this;
  }

  @Override
  public int boardStopPosition() {
    return boardStopPosition;
  }

  @Override
  public T trip() {
    return trip;
  }

  public int boardTime() {
    return trip.departure(boardStopPosition);
  }

  @Override
  public boolean arrivedOnBoard() {
    return true;
  }

  private int boardStopIndex() {
    return trip.pattern().stopIndex(boardStopPosition);
  }
}
