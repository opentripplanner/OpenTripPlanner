package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view;

import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Implement the {@link ArrivalView}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
abstract class StopArrivalViewAdapter<T extends RaptorTripSchedule> implements ArrivalView<T> {

  private final int round;
  private final int stop;

  StopArrivalViewAdapter(int round, int stop) {
    this.round = round;
    this.stop = stop;
  }

  @Override
  public int stop() {
    return stop;
  }

  @Override
  public int round() {
    return round;
  }

  @Override
  public String toString() {
    return asString();
  }
}
