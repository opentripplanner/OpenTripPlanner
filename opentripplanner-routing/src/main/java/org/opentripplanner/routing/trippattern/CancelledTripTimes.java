package org.opentripplanner.routing.trippattern;

/**
 * A CancelledTripTimes represents the cancellation of an entire trip by reporting that the vehicle
 * has already passed all stops.
 */
public class CancelledTripTimes extends DelegatingTripTimes {

    public CancelledTripTimes(ScheduledTripTimes sched) {
        super(sched);
    }

    @Override public int getDepartureTime(int hop) {
        return TripTimes.PASSED;
    }
    
    @Override public int getArrivalTime(int hop) {
        return TripTimes.PASSED;
    }
        
}
