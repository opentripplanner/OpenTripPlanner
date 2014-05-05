package org.opentripplanner.profile;

import lombok.EqualsAndHashCode;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;

/**
 * A ride between two stops on a single pattern. One or more PatternRides can be included in a Ride.
 * Each Pattern needs its own separate PatternRide because stop indexes may be different on every
 * pattern of a route, and we need to store indexes rather than stop objects because a stop can
 * appear more than once in a pattern. When a ride is unfinished (waiting in the queue for
 * exploration in the next round) its toIndex is -1 and its stats field is null.
 * 
 * The hash code and equals method allow adding the same PatternRide to a set multiple times, but
 * coming from different transfers.
 */
@EqualsAndHashCode(exclude="xfer")
class PatternRide {
    
    TripPattern pattern; // uses identity hash code
    int fromIndex;
    int toIndex = -1;
    Ride previous;
    ProfileTransfer xfer; // how did we get here
    Stats stats = null;
    
    /** Construct an unfinished PatternRide, lacking a toIndex and stats. */
    public PatternRide (TripPattern pattern, int fromIndex, Ride previous, ProfileTransfer xfer) {
        this.pattern   = pattern;
        this.fromIndex = fromIndex;
        this.previous  = previous;
        this.xfer      = xfer;
    }
    
    /** Construct an unfinished PatternRide (lacking toIndex and stats) from another PatternRide. */
    public PatternRide (PatternRide other) {
        this.pattern   = other.pattern;
        this.fromIndex = other.fromIndex;
        this.previous  = other.previous;
        this.xfer      = other.xfer;
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
        return String.format("%s from %d, prev is %s", pattern.getCode(), fromIndex, previous);
    }
    
    /** Complete an unfinished ride as a new object. */
    // Perhaps we should check in advance whether a pattern is running at all, but results will vary
    // depending on the fromIndex and toIndex. Some indexes can be reached within the window, others
    // not. But that's an optimization.
    public PatternRide extendToIndex(int toIndex, TimeWindow window) {
        Stats stats = Stats.create (pattern, fromIndex, toIndex, window);
        /* There might not be any trips within the time window. */ 
        if (stats == null) return null;
        /* Copy most fields from this unfinished ride. */
        PatternRide ret = new PatternRide(this);
        /* Then set the other fields to complete the ride. */
        ret.toIndex = toIndex;
        ret.stats = stats;
        return ret;
    }

}