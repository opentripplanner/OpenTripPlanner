package org.opentripplanner.profile;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.Collection;
import java.util.List;

/**
 * num may be 0 if there are no observations.
 * num will become 1 when adding a scalar or another Stats.
 */
class Stats implements Cloneable {
    
    public int min = 0;
    public int avg = 0;
    public int max = 0;
    public int num = 0;

    /** Construct a new empty Stats containing no values. */
    public Stats () { }

    /** Construct a new Stats for a single int value. */
    public Stats (int loneValue) {
        min = loneValue;
        max = loneValue;
        avg = loneValue;
        num = 1;
    }

    /** Construct a new Stats summarizing the given list of ints. */
    public Stats (int... values) {
        this(Ints.asList(values));
    }

    /** Copy constructor. */
    public Stats (Stats other) {
        if (other != null) {
            this.min = other.min;
            this.avg = other.avg;
            this.max = other.max;
        }
    }

    /**
     * Adds another Stats into this one in place. This is intended to combine them in series, as for legs of a journey.
     * It is not really correct for the average, but min and max values hold and avg is still a useful indicator.
     * @return void to avoid thinking that a new object is created.
     */
    public void add(Stats s) { // TODO maybe should be called 'chain' rather than add
        min += s.min;
        max += s.max;
        avg += s.avg; // This only makes sense when adding successive legs TODO think through in depth
        num = 1;      // Num is poorly defined once addition has occurred
    }

    /** Like add(Stats) but min, max, and avg are all equal. */
    public void add(int x) {
        min += x;
        avg += x;
        max += x;
        num = 1; // it's poorly defined here
    }

    /**
     * Takes StreetSegments for each different access/egress mode and creates a stats describing the range of
     * access/egress times present.
     */
    public void add(Collection<StreetSegment> segs) {
        if (segs == null || segs.isEmpty()) return;
        List<Integer> times = Lists.newArrayList();
        for (StreetSegment seg : segs) times.add(seg.time);
        Stats s = new Stats(times);
        add(s);
    }

    /**
     * Combines another Stats into this one in place. This considers the two Stats to be parallel, as for various trips
     * or patterns making up a single leg of a journey. In this case, the weighted average is correctly computed.
     * @return void to avoid thinking that a new object is created.
     */
    public void merge (Stats other) {
        if (other.min < min) min = other.min;
        if (other.max > max) max = other.max;
        avg = (avg * num + other.avg * other.num) / (num + other.num); // TODO should be float math
        // FIXME num is not updated?
    }

    /**
     * Combines a single value into this stats in place.
     * @return void to indicate that a new object is NOT created.
     */
    public void merge (int other) {
        if (other < min) min = other;
        if (other > max) max = other;
        avg = (avg * num + other) / (num + 1); // TODO should be float math
        num += 1;
    }

    /** Build a composite Stats out of a bunch of other Stats. They are combined in parallel, as in merge(Stats). */
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

    /** Construct a Stats containing the min, max, average, and count of the given ints. */
    public Stats (Collection<Integer> ints) {
        if (ints == null || ints.isEmpty()) throw new AssertionError("Stats are undefined if there are no values.");
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
    public static Stats create (TripPattern pattern, int stop0, int stop1, TimeWindow window) {
        Stats s = new Stats ();
        s.min = Integer.MAX_VALUE;
        s.num = 0;
        /* Scan through all non-frequency trips accumulating them into stats. */
        // TODO maybe we should prefilter the triptimes so we aren't constantly iterating over
        // the trips whose service is not running
        for (TripTimes tripTimes : pattern.scheduledTimetable.tripTimes) {
            int depart = tripTimes.getDepartureTime(stop0);
            int arrive = tripTimes.getArrivalTime(stop1);
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
        /* Do the same thing for any frequency-based trips. */
        for (FrequencyEntry freq : pattern.scheduledTimetable.frequencyEntries) {
            TripTimes tt = freq.tripTimes;
            int overlap = window.overlap(freq.startTime, freq.endTime, tt.serviceCode);
            if (overlap == 0) continue;
            int n = overlap / freq.headway + 1; // number of trip instances in the overlap. round up, avoid zeros.
            int depart = tt.getDepartureTime(stop0);
            int arrive = tt.getArrivalTime(stop1);
            int t = arrive - depart;
            if (t < s.min) s.min = t;
            if (t > s.max) s.max = t;
            s.avg += (t * n);
            s.num += n;
        }
        if (s.num > 0) {
            s.avg /= s.num;
            return s;
        }
        /* There are no running trips within the time range, on the given serviceIds. */
        return null;
    }

    @Override
    public String toString() {
        return String.format("min=%.1f avg=%.1f max=%.1f", min/60.0, avg/60.0, max/60.0);
    }
}