package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c2;

import static org.opentripplanner.raptor.api.model.PathLegType.ACCESS;

import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.AccessPathView;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;

/**
 * Represent a access stop arrival.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class AccessStopArrivalC2<T extends RaptorTripSchedule> extends AbstractStopArrivalC2<T> {

  private final RaptorAccessEgress access;

  AccessStopArrivalC2(int departureTime, RaptorAccessEgress access) {
    super(
      access.stop(),
      departureTime,
      access.durationInSeconds(),
      access.numberOfRides(),
      access.c1(),
      RaptorCostCalculator.ZERO_COST
    );
    this.access = access;
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
  public AbstractStopArrivalC2<T> timeShiftNewArrivalTime(int newRequestedArrivalTime) {
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

    return new AccessStopArrivalC2<>(newDepartureTime, access);
  }

  @Override
  public boolean arrivedOnBoard() {
    return access.stopReachedOnBoard();
  }
}
