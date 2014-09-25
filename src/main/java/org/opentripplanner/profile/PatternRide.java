package org.opentripplanner.profile;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;

/**
 * A ride between two stops on a single pattern. One or more PatternRides can be included in a Ride.
 * Each Pattern needs its own separate PatternRide because stop indexes may be different on every
 * pattern of a route, and we need to store indexes rather than stop objects because a stop can
 * appear more than once in a pattern. When a ride is unfinished (waiting in the queue for
 * exploration in the next round) its toIndex is -1 and its stats field is null.
 */
class PatternRide {
    
    final TripPattern pattern; // TripPatterns use identity equality
    final int fromIndex;       // Always set, even on unfinished PatternRides
    final int toIndex;  // set when PatternRide is finished by extending it to a specific stop
    final Stats stats;  // set when PatternRide is finished by extending it to a specific stop
    
    @Override
    public boolean equals(Object o){
    	if(o==this) return true;
    	if (!(o instanceof PatternRide)) return false;
    	PatternRide other = (PatternRide) o;
    	if(!other.pattern.equals(this.pattern)) return false;
    	if(other.fromIndex != fromIndex) return false;
    	if(other.toIndex != toIndex) return false;
    	if(!other.stats.equals(this.stats)) return false;
    	return true;
    }

    /** Construct an unfinished PatternRide, lacking a toIndex and stats. */
    public PatternRide (TripPattern pattern, int fromIndex) {
        this.pattern   = pattern;
        this.fromIndex = fromIndex;
        this.toIndex = -1;
        this.stats = null;
    }

    /** Construct an unfinished PatternRide (lacking toIndex and stats) from another PatternRide. */
    public PatternRide (PatternRide other, int toIndex, Stats stats) {
        this.pattern   = other.pattern;
        this.fromIndex = other.fromIndex;
        this.toIndex = toIndex;
        this.stats = stats;
    }
    
    public Stop getFromStop() {
        return pattern.getStops().get(fromIndex);
    }
    
    public Stop getToStop() {
        return pattern.getStops().get(toIndex);            
    }
    
    public boolean finished () {
        return toIndex >= 0 && stats != null;
    }
    
    public String toString () {
        return String.format("%s %d to %d", pattern.code, fromIndex, toIndex);
    }

    /**
     * Complete an unfinished ride as a new object.
     * Perhaps we should check in advance whether a pattern is running at all, but results will vary depending on the
     * fromIndex and toIndex. Some indexes can be reached within the window, others not. But that's an optimization.
     * It's actually advantageous to call this sequentially for each to-stop instead of incrementally along a pattern.
     * This is because we store the absolute times of arrivals and departures, and by subtracting those we get accurate
     * min and max ride times rather than looser lower and upper bounds on travel time.
     */
    public PatternRide extendToIndex(int toIndex, TimeWindow window) {
        Stats stats = Stats.create (pattern, fromIndex, toIndex, window);
        /* There might not be any trips within the time window. */ 
        if (stats == null) return null;
        /* Copy most fields from this unfinished ride, setting the other fields to complete the ride. */
        PatternRide ret = new PatternRide(this, toIndex, stats);
        return ret;
    }

}