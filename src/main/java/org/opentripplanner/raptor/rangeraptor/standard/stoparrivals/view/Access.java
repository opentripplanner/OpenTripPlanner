package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view;

import org.opentripplanner.raptor.api.view.AccessPathView;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

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
  public int arrivalTime() {
    return arrivalTime;
  }

  @Override
  public ArrivalView<T> previous() {
    throw new UnsupportedOperationException("Access path arrival is the first path.");
  }

  @Override
  public boolean arrivedByAccess() {
    return true;
  }

  @Override
  public AccessPathView accessPath() {
    return this;
  }

  @Override
  public RaptorAccessEgress access() {
    return access;
  }
}
