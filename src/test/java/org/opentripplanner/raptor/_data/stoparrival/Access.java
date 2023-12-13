package org.opentripplanner.raptor._data.stoparrival;

import static org.opentripplanner.raptor.api.model.PathLegType.ACCESS;

import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.view.AccessPathView;

class Access extends AbstractStopArrival {

  private final RaptorAccessEgress access;

  Access(int stop, int arrivalTime, RaptorAccessEgress path, int c2) {
    super(0, stop, arrivalTime, path.c1(), c2, null);
    this.access = path;
  }

  @Override
  public PathLegType arrivedBy() {
    return ACCESS;
  }

  @Override
  public AccessPathView accessPath() {
    return () -> access;
  }

  @Override
  public boolean arrivedOnBoard() {
    return access.stopReachedOnBoard();
  }
}
