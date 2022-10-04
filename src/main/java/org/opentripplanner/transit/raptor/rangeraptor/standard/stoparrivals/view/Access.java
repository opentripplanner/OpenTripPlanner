package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorAccessEgress;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.AccessPathView;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;

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
