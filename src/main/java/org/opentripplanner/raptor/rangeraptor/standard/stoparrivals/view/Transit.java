package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view;

import static org.opentripplanner.raptor.api.model.PathLegType.TRANSIT;

import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.TransitPathView;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.StopArrivalState;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;

final class Transit<T extends RaptorTripSchedule>
  extends StopArrivalViewAdapter<T>
  implements TransitPathView<T> {

  private final StopArrivalState<T> arrival;
  private final StopsCursor<T> cursor;

  Transit(int round, int stop, StopArrivalState<T> arrival, StopsCursor<T> cursor) {
    super(round, stop);
    this.arrival = arrival;
    this.cursor = cursor;
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
    return arrival.onBoardArrivalTime();
  }

  @Override
  public ArrivalView<T> previous() {
    return cursor.stop(round() - 1, boardStop(), this);
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
  public int boardStop() {
    return arrival.boardStop();
  }

  @Override
  public T trip() {
    return arrival.trip();
  }

  public int boardTime() {
    return arrival.boardTime();
  }

  @Override
  public boolean arrivedOnBoard() {
    return true;
  }
}
