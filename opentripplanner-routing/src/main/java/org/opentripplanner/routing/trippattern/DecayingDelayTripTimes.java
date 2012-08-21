package org.opentripplanner.routing.trippattern;

/**
 * An DelayedTripTimes applies an offset to arrival and departure times based on a report that a
 * vehicle is a given number of seconds early or late, and reports that the vehicle has 
 * passed all stops up to a certain point based on a report of vehicle location.
 */
public class DecayingDelayTripTimes extends DelegatingTripTimes implements TripTimes {

    private final int currentStop;
    private final int delay;
    private final double k;
    // compute decay lookup table?
    
    public DecayingDelayTripTimes(ScheduledTripTimes sched, int currentStop, int delay, double decay) {
        super(sched);
        this.delay = delay;
        this.currentStop = currentStop;
        this.k = decay;
    }

    @Override public int getDepartureTime(int hop) {
        int stop = hop;
        if (stop < currentStop)
            return TripTimes.PASSED;
        return super.getDepartureTime(hop) + decayedDelay(stop);
    }
    
    @Override public int getArrivalTime(int hop) {
        int stop = hop + 1;
        if (stop < currentStop)
            return TripTimes.PASSED;
        return super.getArrivalTime(hop) + decayedDelay(stop);
    }
        
    private int decayedDelay(int stop) {
        int n = stop - currentStop;
        double decay = Math.pow(k, n);
        return (int) (decay * delay);
    }
    
}
