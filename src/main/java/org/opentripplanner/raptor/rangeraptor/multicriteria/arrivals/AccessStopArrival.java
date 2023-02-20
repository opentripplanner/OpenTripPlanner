package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.raptor.api.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.AccessPathView;

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

    if (newArrivalTime == RaptorConstants.TIME_NOT_SET) {
      throw new IllegalStateException(
        "The arrival should not have been accepted if it does not have a legal arrival-time."
      );
    }
    if (newArrivalTime == arrivalTime()) {
      return this;
    }
    int newDepartureTime = newArrivalTime - access.durationInSeconds();

    return new AccessStopArrival<>(newDepartureTime, access);
  }
}
