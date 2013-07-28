/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.trippattern;

import lombok.val;

/**
 * An DelayedTripTimes applies an offset to arrival and departure times based on a report that a
 * vehicle is a given number of seconds early or late, and reports that the vehicle has 
 * passed all stops up to a certain point based on a report of vehicle location.
 */
public class DecayingDelayTripTimes extends DelegatingTripTimes {

    private final int currentStop;
    private final int t0;
    private final int delay;
    private final boolean linear;
    private final boolean readThrough;
    private final double lambda;

    public DecayingDelayTripTimes(ScheduledTripTimes sched, int currentStop, int delay) {
        this(sched, currentStop, delay, 500, false, false);
    }
    
    public DecayingDelayTripTimes(ScheduledTripTimes sched, int currentStop, int delay, 
        double halfLife, boolean linear, boolean readThrough) {
        super(sched);
        this.t0 = sched.getDepartureTime(currentStop);
        this.delay = delay;
        this.currentStop = currentStop;
        this.linear = linear;
        this.readThrough = readThrough;
        this.lambda = 1.0/halfLife;
    }

    @Override public int getDepartureTime(int hop) {
        int stop = hop;
        if (stop < currentStop) {
            if (readThrough)
                return super.getDepartureTime(hop);
            return TripTimes.PASSED;
        }
        int t = super.getDepartureTime(hop);
        int elapsed = t - t0;
        return t + decayedDelay(elapsed);
    }
    
    @Override public int getArrivalTime(int hop) {
        int stop = hop + 1;
        if (stop < currentStop) {
            if (readThrough)
                return super.getArrivalTime(hop);
            return TripTimes.PASSED;
        }
        int t = super.getArrivalTime(hop);
        int elapsed = t - t0;
        return t + decayedDelay(elapsed);
    }
        
    private int decayedDelay(int dt) {
        if (delay == 0) 
            return 0;
        // This would make the decay symmetric about the current stop. Not currently needed, as 
        // we are reporting PASSED for all stops before currentStop.
        // n = Math.abs(n);
        double decay;
        if (linear) {
            decay = 1 - dt * lambda * 0.5;
            if (decay < 0)
                decay = 0;
        } else {
            decay = Math.exp(-lambda * dt);
        }
        return (int) (decay * delay);
    }
    
    @Override public String toString() {
        val sb = new StringBuilder();
        sb.append(String.format("%s DecayingDelayTripTimes delay=%d stop=%d halfLife=%03.1f\n", 
                linear ? "Linear" : "Exponential", delay, currentStop, 1/lambda));
        for (int i = 0; i < getNumHops(); i++) {
            int td = super.getDepartureTime(i);
            int ed = td - t0;
            int ta = super.getArrivalTime(i);
            int ea = ta - t0;
            int dd = 0;
            int da = 0;
            if (i >= currentStop) {
                dd = decayedDelay(ed);
                da = decayedDelay(ea);
            }
            String s = String.format("(%d)%5d  %5d", i, dd, da);
            sb.append(s);
        }
        sb.append('\n');
        sb.append(dumpTimes());
        sb.append("\nbased on: ");
        sb.append(super.toString());
        return sb.toString();
    }

    @Override public boolean compact() {
        // Nothing much to compact. Maybe compute a decay lookup table?
        return false;
    }
    
}
