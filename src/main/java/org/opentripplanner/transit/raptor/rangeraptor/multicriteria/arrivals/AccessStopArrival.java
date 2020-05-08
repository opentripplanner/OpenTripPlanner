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

    public AccessStopArrival(int departureTime, int cost, RaptorTransfer access) {
        super(access.stop(), departureTime, access.durationInSeconds(), cost);
        this.access = access;
    }

    @Override
    public RaptorTransfer accessEgress() {
        return access;
    }

    public boolean arrivedByAccessLeg() {
        return true;
    }

    @Override
    public AbstractStopArrival<T> timeShiftNewArrivalTime(int newRequestedArrivalTime) {
        int latestArrivalTime = access.latestArrivalTime(newRequestedArrivalTime);

        if(latestArrivalTime == -1) { return this; }

        int timeShift = latestArrivalTime - arrivalTime();

        return timeShift == 0
            ? this
            : new AccessStopArrival<>(departureTime() + timeShift, cost(), access);
    }
}
