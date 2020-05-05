package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;


import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * Represent a access stop arrival.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class AccessStopArrival<T extends RaptorTripSchedule> extends AbstractStopArrival<T> {
    private final RaptorTransfer access;

    public AccessStopArrival(
        int stop,
        int departureTime,
        int accessDurationInSeconds,
        int cost,
        RaptorTransfer access
    ) {
        super(
                stop,
                departureTime,
                departureTime + accessDurationInSeconds,
                accessDurationInSeconds,
                cost
        );
        this.access = access;
    }

    @Override
    public RaptorTransfer accessEgress() {
        return access;
    }

    public boolean arrivedByAccessLeg() {
        return true;
    }
}
