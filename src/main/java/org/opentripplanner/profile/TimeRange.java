package org.opentripplanner.profile;

import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TimeRange {

    public int min, max, avg, n;

    /** Construct a TimeRange where all fields are zero. */
    public TimeRange() { };

    /** Construct a TimeRange for a single value. */
    public TimeRange(int t) {
        this.min = t;
        this.max = t;
        this.avg = t;
        n = 1;
    }

    /** Return true if the time range was updated, false if it remained the same. */
    // TODO does comparing averages lead to an endless-improvement loop situation?
    public boolean mergeIn (TimeRange other) {
        if (other.min < this.min || other.max < this.max || other.avg < this.avg) {
            // the other range is better in at least one way, combine it into this one
            if (other.min < this.min && other.max < this.max && other.avg < this.avg) {
                // the other range completely dominates the existing one, replacing it entirely
                this.min = other.min;
                this.max = other.max;
                this.avg = other.avg;
                this.n = other.n;
                return true;
            }
            if (other.min < this.min) this.min = other.min;
            if (other.max < this.max) this.max = other.max; // Yes, we want the minimum upper bound.
            // Watch out for overflow here.
//            double newAverage = this.avg * (double) this.n + other.avg * (double) other.n;
//            this.n += other.n;
//            newAverage /= this.n;
//            this.avg = (int) newAverage;
            // This assumes all the mixed distributions are symmetric, which they are not. But just to get some coherent value...
            this.avg = (int) (((double) this.min + (double) this.max) / 2.0d);
            checkCoherent();
            return true; // We know at least one field was updated.
        } else {
            // the other range is worse in every way, ignore it
            return false;
        }
    }

    /** Return a copy of this TimeRange that is translated forward in time by t seconds. */
    public TimeRange shift (int t) {
        TimeRange ret = new TimeRange();
        ret.min = this.min + t;
        ret.max = this.max + t;
        ret.avg = this.avg + t;
        ret.n = this.n;
        return ret;
    }

    /** Return a copy of this TimeRange that includes a uniformly distributed wait from zero to t seconds. */
    public TimeRange wait (int t) {
        TimeRange ret = new TimeRange();
        ret.min = this.min;
        ret.max = this.max + t;
        ret.avg = this.avg + t/2;
        ret.n = this.n;
        return ret;
    }

    /** Keeps one TimeRange per TransitStop that has been reached. */
    public static class Tracker implements Iterable<Stop> {

        Map<Stop, TimeRange> ranges = Maps.newHashMap();

        /** Set the travel time to a specific transit stop to exactly t seconds, overwriting any existing value. */
        public void set (Stop stop, int t) {
            ranges.put(stop, new TimeRange(t));
        }

        /** Get the existing TimeRange for the specified stop, or NULL if none is defined. */
        public TimeRange get (Stop stop) {
            return ranges.get(stop);
        }

        /** Return true if the time range at the given stop was updated. */
        public boolean add (Stop stop, TimeRange newRange) {
            TimeRange existingRange = ranges.get(stop);
            if (existingRange == null) {
                ranges.put(stop, newRange);
                return true;
            }
            return existingRange.mergeIn(newRange);
        }

        @Override
        public Iterator<Stop> iterator() {
            return ranges.keySet().iterator();
        }
    }

    public void checkCoherent() {
        if (avg < 0) {
            System.out.printf ("avg is negative: %d \n", avg);
        }
        if (! (min <= avg && avg <= max)) {
            System.out.printf("incoherent: min %d avg %d max %d \n", min, avg, max);
        }
    }

}
