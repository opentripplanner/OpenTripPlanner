package org.opentripplanner.raptor.spi;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface TransitArrival<T extends RaptorTripSchedule> {
  static <T extends RaptorTripSchedule> TransitArrival<T> create(
    final T trip,
    final int stop,
    final int time
  ) {
    return new TransitArrival<>() {
      @Override
      public T trip() {
        return trip;
      }

      @Override
      public int stop() {
        return stop;
      }

      @Override
      public int arrivalTime() {
        return time;
      }
    };
  }

  T trip();

  int stop();

  int arrivalTime();
}
