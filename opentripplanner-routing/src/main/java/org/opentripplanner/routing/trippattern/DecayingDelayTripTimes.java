package org.opentripplanner.routing.trippattern;

import lombok.val;

/**
 * An DelayedTripTimes applies an offset to arrival and departure times based on a report that a
 * vehicle is a given number of seconds early or late, and reports that the vehicle has 
 * passed all stops up to a certain point based on a report of vehicle location.
 */
public class DecayingDelayTripTimes extends DelegatingTripTimes {

    private final int currentStop;
    private final int delay;
    private final double k;
    private final boolean linear;
    private final boolean readThrough;

    public DecayingDelayTripTimes(ScheduledTripTimes sched, int currentStop, int delay) {
        this(sched, currentStop, delay, 0.7, false, false);
    }
    
    public DecayingDelayTripTimes(ScheduledTripTimes sched, int currentStop, int delay, 
        double decayParam, boolean linear, boolean readThrough) {
        super(sched);
        this.delay = delay;
        this.currentStop = currentStop;
        this.k = decayParam;
        this.linear = linear;
        this.readThrough = readThrough;
    }

    @Override public int getDepartureTime(int hop) {
        int stop = hop;
        if (stop < currentStop) {
            if (readThrough)
                return super.getDepartureTime(hop);
            return TripTimes.PASSED;
        }
        return super.getDepartureTime(hop) + decayedDelay(stop);
    }
    
    @Override public int getArrivalTime(int hop) {
        int stop = hop + 1;
        if (stop < currentStop) {
            if (readThrough)
                return super.getArrivalTime(hop);
            return TripTimes.PASSED;
        }
        return super.getArrivalTime(hop) + decayedDelay(stop);
    }
        
    private int decayedDelay(int stop) {
        if (delay == 0) 
            return 0;
        int n = stop - currentStop;
        // This would make the decay symmetric about the current stop. Not currently needed, as 
        // we are reporting PASSED for all stops before currentStop.
        // n = Math.abs(n);
        double decay;
        if (linear)
            decay = (n > k) ? 0.0 : 1.0 - n / k;
        else  
            decay = Math.pow(k, n);
        return (int) (decay * delay);
    }
    
    @Override public String toString() {
        val sb = new StringBuilder();
        sb.append(String.format("%s DecayingDelayTripTimes delay=%d stop=%d param=%3.2f\n", 
                linear ? "Linear" : "Exponential", delay, currentStop, k));
        for (int i = 0; i < getNumHops(); i++) {
            sb.append(i);
            sb.append(':');
            int j = 0;
            if (i >= currentStop)
                j = decayedDelay(i);
            sb.append(j);
            sb.append(' ');
        }
        sb.append(dumpTimes());
        sb.append("\nbased on:\n");
        sb.append(super.toString());
        return sb.toString();
    }

    @Override public boolean compact() {
        // Nothing much to compact. Maybe compute a decay lookup table?
        return false;
    }
    
}
