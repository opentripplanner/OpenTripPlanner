package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view;

import static org.opentripplanner.raptor.api.model.PathLegType.ACCESS;

import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.AccessPathView;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;

final class Access<T extends RaptorTripSchedule>
  extends StopArrivalViewAdapter<T>
  implements AccessPathView {

  private final int arrivalTime;
  private final RaptorAccessEgress access;

  Access(int round, int arrivalTime, RaptorAccessEgress access) {
    super(round, access.stop());
    this.arrivalTime = arrivalTime;
    this.access = access;
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
    return null;
  }

  @Override
  public PathLegType arrivedBy() {
    return ACCESS;
  }

  @Override
  public AccessPathView accessPath() {
    return this;
  }

  @Override
  public RaptorAccessEgress access() {
    return access;
  }

  @Override
  public boolean arrivedOnBoard() {
    return access.stopReachedOnBoard();
  }
}
