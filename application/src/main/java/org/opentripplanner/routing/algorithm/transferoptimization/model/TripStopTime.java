package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TripStopTime<T extends RaptorTripSchedule> implements StopTime {

  private final T trip;
  private final int stopPosition;
  private final boolean departure;

  private TripStopTime(T trip, int stopPosition, boolean departure) {
    assertStopPositionIsInRange(stopPosition, trip);
    this.trip = trip;
    this.stopPosition = stopPosition;
    this.departure = departure;
  }

  public static <T extends RaptorTripSchedule> TripStopTime<T> arrival(T trip, int stopPosition) {
    return new TripStopTime<>(trip, stopPosition, false);
  }

  public static <T extends RaptorTripSchedule> TripStopTime<T> arrival(T trip, StopTime stopTime) {
    int stopPosition = trip.findArrivalStopPosition(stopTime.time(), stopTime.stop());
    return arrival(trip, stopPosition);
  }

  public static <T extends RaptorTripSchedule> TripStopTime<T> departure(T trip, int stopPosition) {
    return new TripStopTime<>(trip, stopPosition, true);
  }

  public static <T extends RaptorTripSchedule> TripStopTime<T> departure(
    T trip,
    StopTime stopTime
  ) {
    int stopPosition = trip.findDepartureStopPosition(stopTime.time(), stopTime.stop());
    return departure(trip, stopPosition);
  }

  public T trip() {
    return trip;
  }

  public int stopPosition() {
    return stopPosition;
  }

  public int stop() {
    return trip.pattern().stopIndex(stopPosition);
  }

  public int time() {
    return departure ? trip.departure(stopPosition) : trip.arrival(stopPosition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trip, stopPosition, departure);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TripStopTime<?> that = (TripStopTime<?>) o;

    return (
      stopPosition == that.stopPosition && departure == that.departure && trip.equals(that.trip)
    );
  }

  @Override
  public String toString() {
    return ValueObjectToStringBuilder.of()
      .addText("[")
      .addNum(stop())
      .addText(" ")
      .addServiceTime(time())
      .addText(" ")
      .addObj(trip.pattern().debugInfo())
      .addText("]")
      .toString();
  }

  private void assertStopPositionIsInRange(int stopPosition, T trip) {
    if (stopPosition < 0 || stopPosition >= trip.pattern().numberOfStopsInPattern()) {
      throw new IndexOutOfBoundsException();
    }
  }
}
