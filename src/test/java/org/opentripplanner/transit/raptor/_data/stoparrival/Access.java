package org.opentripplanner.transit.raptor._data.stoparrival;

import org.opentripplanner.transit.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.transit.raptor.api.transit.AccessEgress;
import org.opentripplanner.transit.raptor.api.view.AccessPathView;

public class Access extends AbstractStopArrival {

  private final AccessEgress access;

  public Access(int stop, int departureTime, int arrivalTime, int cost) {
    this(
      stop,
      arrivalTime,
      TestAccessEgress.walkAccessEgress(stop, Math.abs(arrivalTime - departureTime), cost)
    );
  }

  public Access(int stop, int arrivalTime, AccessEgress path) {
    super(0, stop, arrivalTime, path.generalizedCost(), null);
    this.access = path;
  }

  @Override
  public boolean arrivedByAccess() {
    return true;
  }

  @Override
  public AccessPathView accessPath() {
    return () -> access;
  }
}
