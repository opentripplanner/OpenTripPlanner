package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c1;

import static org.opentripplanner.raptor.api.model.PathLegType.ACCESS;

import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.AccessPathView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;

/**
 * Represent a access stop arrival.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class AccessStopArrival<T extends RaptorTripSchedule> extends McStopArrival<T> {

  private final RaptorAccessEgress access;

  AccessStopArrival(int departureTime, RaptorAccessEgress access) {
    super(
      access.stop(),
      departureTime,
      access.durationInSeconds(),
      access.c1(),
      access.numberOfRides()
    );
    this.access = access;
  }

  @Override
  public int c2() {
    return RaptorConstants.NOT_SET;
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
  public McStopArrival<T> timeShiftNewArrivalTime(int newRequestedArrivalTime) {
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

  @Override
  public boolean arrivedOnBoard() {
    return access.stopReachedOnBoard();
  }
}
