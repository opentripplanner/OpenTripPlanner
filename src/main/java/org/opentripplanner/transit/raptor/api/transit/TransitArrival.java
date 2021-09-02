package org.opentripplanner.transit.raptor.api.transit;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface TransitArrival<T extends RaptorTripSchedule> {
    T trip();
    int stop();
    int arrivalTime();

    static <T extends RaptorTripSchedule> TransitArrival<T> create(
            final T trip,
            final int stop,
            final int time
    ) {
        return new TransitArrival<>() {
            @Override public T trip() { return trip; }
            @Override public int stop() { return stop; }
            @Override public int arrivalTime() { return time; }
        };
    }
}
