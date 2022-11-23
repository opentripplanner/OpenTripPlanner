package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.raptor.api.view.TransitPathView;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.TransitArrival;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransitStopArrival<T extends RaptorTripSchedule>
  extends AbstractStopArrival<T>
  implements TransitPathView<T>, TransitArrival<T> {

  private final T trip;

  public TransitStopArrival(
    AbstractStopArrival<T> previousState,
    int stopIndex,
    int arrivalTime,
    int totalCost,
    T trip
  ) {
    super(
      previousState,
      previousState.arrivedByTransit() ? 2 : 1,
      stopIndex,
      arrivalTime,
      totalCost
    );
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
}
