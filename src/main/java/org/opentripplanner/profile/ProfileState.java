package org.opentripplanner.profile;

import java.util.Collection;
import java.util.Iterator;
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
    public TripPattern[] patterns;
    
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
        
        ret.targetPatterns = null;
        
        return ret;
    }
    
    /**
     * Propagate this state along a segment with a definite time (e.g. a street segment)
     */
    public ProfileState propagate (int delta) {
        return propagate(delta, delta);
    }
    
    /** two ways to create a profile state: the initial state, reached via an on-street mode, and subsequent states reached via transit */
    public static enum Type {
        STREET, TRANSIT, TRANSFER;
    }

    /** Merge the other profile state into this one, in place */
    public void mergeIn(ProfileState other) {
        if (this.lowerBound < 0 || other.lowerBound < 0 || this.upperBound < 0 || other.upperBound < 0)
            throw new IllegalStateException("Invalid bound");
        
        this.lowerBound = Math.min(this.lowerBound, other.lowerBound);
        // the upper bound of a common trunk is the _minimum_ upper bound of all its constituents
        this.upperBound = Math.min(this.upperBound, other.upperBound);        
    }

    public boolean containsPattern(TripPattern pattern) {
        if (patterns == null)
            return false;
        
        for (TripPattern tp : patterns) {
            if (tp == pattern)
                return true;
        }
        
        return false;
    }
    
    
    /** merge all the profile states into a new ProfileState. Assumes that each state consists of a single pattern unique among the states. */
    public static ProfileState merge (Collection<ProfileState> states, boolean retainPatterns) {
        ProfileState ret = new ProfileState();
        ret.lowerBound = ret.upperBound = Integer.MAX_VALUE;
        
        if (retainPatterns)
            ret.patterns = new TripPattern[states.size()];
        
        {
            int i = 0;
            for (Iterator<ProfileState> it = states.iterator(); it.hasNext(); i++) {
                ProfileState state = it.next();
                
                if (ret.stop == null) ret.stop = state.stop;
                
                if (state.lowerBound < ret.lowerBound) ret.lowerBound = state.lowerBound;
                
                // Yes, we want the _minimum_ upper bound: you will never take a journey that is longer than the minimum
                // upper bound, in either the perfect information case or the min-upper-bound case.
                if (state.upperBound < ret.upperBound) ret.upperBound = state.upperBound;
                
                if (retainPatterns)
                    ret.patterns[i] = state.patterns[0];
            }
        }
        
        return ret;
    }
}
