package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.Objects;
import org.opentripplanner.model.base.ValueObjectToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public final class TripStopTime<T extends RaptorTripSchedule> implements StopTime {
  private final T trip;
  private final int stopPosition;
  private final boolean departure;

  private TripStopTime(T trip, int stopPosition, boolean departure) {
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

  public static <T extends RaptorTripSchedule> TripStopTime<T> departure(T trip, StopTime stopTime) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    TripStopTime<?> that = (TripStopTime<?>) o;

    return stopPosition == that.stopPosition
        && departure == that.departure
        && trip.equals(that.trip);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trip, stopPosition, departure);
  }
}
