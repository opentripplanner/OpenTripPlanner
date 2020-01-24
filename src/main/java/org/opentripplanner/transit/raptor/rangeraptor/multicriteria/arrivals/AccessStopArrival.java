package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;


import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;


/**
 * Represent a access stop arrival.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class AccessStopArrival<T extends RaptorTripSchedule> extends AbstractStopArrival<T> {
    private final int accessDurationInSeconds;
    private final TransitCalculator calculator;

    public AccessStopArrival(int stop, int departureTime, int accessDurationInSeconds, int cost, TransitCalculator calculator) {
        super(
                stop,
                departureTime,
                departureTime + accessDurationInSeconds,
                accessDurationInSeconds,
                cost
        );
        this.calculator = calculator;
        this.accessDurationInSeconds = accessDurationInSeconds;
    }

    public boolean arrivedByAccessLeg() {
        return true;
    }
}
