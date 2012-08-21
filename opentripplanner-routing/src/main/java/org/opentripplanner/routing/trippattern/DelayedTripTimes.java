package org.opentripplanner.routing.trippattern;

/**
 * An DelayedTripTimes applies an offset to arrival and departure times based on a report that a
 * vehicle is a given number of seconds early or late, and optionally reports that the vehicle has 
 * passed all stops up to a certain point based on a report of vehicle location.
 */
public class DelayedTripTimes extends DelegatingTripTimes implements TripTimes {

    private final int currentStop;
    
    private final int delay;
    
    public DelayedTripTimes(ScheduledTripTimes sched, int delay, int currentStop) {
        super(sched);
        this.delay = delay;
        this.currentStop = currentStop;
    }

    @Override public int getDepartureTime(int hop) {
        int stop = hop;
        if (stop < currentStop)
            return TripTimes.PASSED;
        return super.getDepartureTime(hop) + delay;
    }
    
    @Override public int getArrivalTime(int hop) {
        int stop = hop + 1;
        if (stop < currentStop)
            return TripTimes.PASSED;
        return super.getArrivalTime(hop) + delay;
    }
        
}
