package org.opentripplanner.profile;

import java.util.Collection;

import lombok.Getter;

import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * num may be 0 if there are no observations.
 * num will become 1 when adding a scalar or another Stats.
 */
class Stats implements Cloneable {
    
    @Getter int min = 0;
    @Getter int avg = 0;
    @Getter int max = 0;
    @Getter int num = 0;
    
    public Stats () { }
    
    public Stats (Stats other) {
        if (other != null) {
            this.min = other.min;
            this.avg = other.avg;
            this.max = other.max;
        }
    }
    
    public void add(Stats s) {
        min += s.min;
        max += s.max;
        avg += s.avg; // This only makes sense when adding successive legs TODO think through in depth
        num = 1;      // Num is poorly defined once addition has occurred
    }

    /**
     * We define adding an int I into a Stats to mean adding a Stats where min, max, and avg are all equal to I.
     */
    public void add(int x) {
        min += x;
        avg += x;
        max += x;
        num = 1; // it's poorly defined here
    }
    
    public void merge (Stats other) {
        if (other.min < min) min = other.min;
        if (other.max > max) max = other.max;
        avg = (avg * num + other.avg * other.num) / (num + other.num); // TODO should be float math
    }
    
    /** Build a composite Stats out of a bunch of other Stats. */
    public Stats (Iterable<Stats> stats) {
        min = Integer.MAX_VALUE;
        num = 0;
        for (Stats other : stats) {
            if (other.min < min) min = other.min;
            if (other.max > max) max = other.max;
            avg += other.avg * other.num;
            num += other.num;
        }
        avg /= num; // TODO should perhaps be float math
    }

    public Stats (Collection<Integer> ints) {
        if (ints == null || ints.isEmpty()) return; // all zeros
        min = Integer.MAX_VALUE;
        double accumulated = 0;
        for (int i : ints) {
            if (i > max) max = i;
            if (i < min) min = i;
            accumulated += i;
        }
        num = ints.size();
        avg = (int) (accumulated / num);
    }
    
    public void dump() {
        System.out.printf("min %d avg %d max %d\n", min, avg, max);
    }
    
    /** Scan through all trips on this pattern and summarize those that are running. */
    public static Stats create (TripPattern pattern, int hop0, int hop1, TimeWindow window) {
        Stats s = new Stats ();
        s.min = Integer.MAX_VALUE;
        s.num = 0;
        for (TripTimes tripTimes : pattern.getScheduledTimetable().getTripTimes()) {
            int depart = tripTimes.getDepartureTime(hop0);
            int arrive = tripTimes.getArrivalTime(hop1);
            if (window.includes (depart) && 
                window.includes (arrive) && 
                window.servicesRunning.get(tripTimes.serviceCode)) {
                int t = arrive - depart;
                if (t < s.min) s.min = t;
                if (t > s.max) s.max = t;
                s.avg += t;            
                ++s.num;
            }
        }
        if (s.num > 0) {
            s.avg /= s.num;
            return s;
        }
        /* There are no running trips within the time range, on the given serviceIds. */
        return null;
    }
    
}