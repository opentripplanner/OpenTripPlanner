package org.opentripplanner.profile;

import java.util.Collection;
import java.util.Set;

import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;

import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.vertextype.TransitStop;

/** represents a state in profile routing */
public class ProfileState implements Cloneable {
    public int lowerBound;
    public int upperBound;
    public Type accessType;
    
    /** the trip patterns used to access this stop */
    public Collection<TripPattern> patterns = Lists.newArrayList();
    
    /** the location of this state */
    public TransitStop stop;
    
    /** the previous state, or null if this is the initial access leg */
    public ProfileState previous;
    
    /** the patterns that should be boarded from this state */
    public Collection<TripPattern> targetPatterns;
    
    /** 
     * Propagate this state along a ride with the given min and max times, to the given transit stop.
     * It is the responsibility of the caller to set accessType and stop appropriately.
     */
    public ProfileState propagate (int deltaMin, int deltaMax) {
        ProfileState ret;
        try {
            ret = (ProfileState) this.clone();
        } catch (CloneNotSupportedException e) {
            // this can't happen
            throw new RuntimeException(e);
        }
        
        ret.previous = this;
        ret.lowerBound += deltaMin;
        ret.upperBound += deltaMax;
        
        return ret;
    }
    
    /**
     * Propagate this state along a segment with a definite time (e.g. a street segment)
     */
    public ProfileState propagate (int delta) {
        return propagate(delta, delta);
    }
    
    public void clearPatterns () {
        this.patterns = Lists.newArrayList();
    }
    
    /** two ways to create a profile state: the initial state, reached via an on-street mode, and subsequent states reached via transit */
    public static enum Type {
        STREET, TRANSIT, TRANSFER;
    }

    /** Merge the other profile state into this one, in place */
    public void mergeIn(ProfileState other) {
        this.lowerBound = Math.min(this.lowerBound, other.lowerBound);
        // the upper bound of a common trunk is the _minimum_ upper bound of all its constituents
        this.upperBound = Math.min(this.upperBound, other.upperBound);
        this.patterns.addAll(other.patterns);
    }
}
