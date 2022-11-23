package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.raptor.api.view.AccessPathView;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Represent a access stop arrival.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class AccessStopArrival<T extends RaptorTripSchedule> extends AbstractStopArrival<T> {

  private final RaptorAccessEgress access;

  public AccessStopArrival(int departureTime, RaptorAccessEgress access) {
    super(
      access.stop(),
      departureTime,
      access.durationInSeconds(),
      access.generalizedCost(),
      access.numberOfRides()
    );
    this.access = access;
  }

  @Override
  public boolean arrivedByAccess() {
    return true;
  }

  @Override
  public AccessPathView accessPath() {
    return () -> access;
  }

  @Override
  public AbstractStopArrival<T> timeShiftNewArrivalTime(int newRequestedArrivalTime) {
    int newArrivalTime = access.latestArrivalTime(newRequestedArrivalTime);

    if (newArrivalTime == -1 || newArrivalTime == arrivalTime()) {
      return this;
    }

    int newDepartureTime = newArrivalTime - access.durationInSeconds();

    return new AccessStopArrival<>(newDepartureTime, access);
  }
}
