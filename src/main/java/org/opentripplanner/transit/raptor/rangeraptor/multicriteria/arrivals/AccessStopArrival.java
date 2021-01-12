package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;


import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.AccessPathView;


/**
 * Represent a access stop arrival.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class AccessStopArrival<T extends RaptorTripSchedule> extends AbstractStopArrival<T> {
    private final RaptorTransfer access;

    public AccessStopArrival(int departureTime, int cost, RaptorTransfer access) {
        super(access.stop(), departureTime, access.durationInSeconds(), cost, access.numberOfRides());
        this.access = access;
    }

    @Override
    public boolean arrivedByAccess() {
        return true;
    }

    @Override
    public AccessPathView accessPath() { return () -> access; }

    @Override
    public AbstractStopArrival<T> timeShiftNewArrivalTime(int newRequestedArrivalTime) {
        int newArrivalTime = access.latestArrivalTime(newRequestedArrivalTime);

        if(newArrivalTime == -1 || newArrivalTime == arrivalTime()) { return this; }

        int newDepartureTime = newArrivalTime - access.durationInSeconds();

        return new AccessStopArrival<>(newDepartureTime, cost(), access);
    }
}
