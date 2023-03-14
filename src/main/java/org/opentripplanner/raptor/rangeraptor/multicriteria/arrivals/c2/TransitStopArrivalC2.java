package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c2;

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

  TransitStopArrivalC2(McStopArrival<T> previous, int stopIndex, int arrivalTime, int c1, T trip) {
    super(previous, previous.arrivedByTransit() ? 2 : 1, stopIndex, arrivalTime, c1, previous.c2());
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
  public boolean arrivedByTransit() {
    return true;
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
