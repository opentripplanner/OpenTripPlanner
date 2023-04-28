package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c2;

import static org.opentripplanner.raptor.api.model.PathLegType.TRANSIT;

import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.TransitArrival;
import org.opentripplanner.raptor.api.view.TransitPathView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class TransitStopArrivalC2<T extends RaptorTripSchedule>
  extends AbstractStopArrivalC2<T>
  implements TransitPathView<T>, TransitArrival<T> {

  private final T trip;

  TransitStopArrivalC2(
    McStopArrival<T> previous,
    int stopIndex,
    int arrivalTime,
    int c1,
    int c2,
    T trip
  ) {
    super(previous, previous.arrivedBy(TRANSIT) ? 2 : 1, stopIndex, arrivalTime, c1, c2);
    this.trip = trip;
  }

  @Override
  public int boardStop() {
    return previousStop();
  }

  @Override
  public T trip() {
    return trip;
  }

  @Override
  public TransitArrival<T> mostRecentTransitArrival() {
    return this;
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
  public boolean arrivedOnBoard() {
    return true;
  }
}
