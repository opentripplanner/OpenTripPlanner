package org.opentripplanner.routing.trippattern;

/**
 * A CanceledTripTimes represents the cancellation of an entire trip by reporting that the vehicle
 * has already passed all stops.
 */
public class CanceledTripTimes extends DelegatingTripTimes {

    public CanceledTripTimes(ScheduledTripTimes sched) {
        super(sched);
    }

    @Override public int getDepartureTime(int hop) {
        return TripTimes.CANCELED;
    }
    
    @Override public int getArrivalTime(int hop) {
        return TripTimes.CANCELED;
    }
        
}
