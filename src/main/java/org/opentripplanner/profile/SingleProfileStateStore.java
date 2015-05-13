package org.opentripplanner.profile;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import jersey.repackaged.com.google.common.collect.Maps;

import org.opentripplanner.routing.vertextype.TransitStop;

/**
 * A ProfileStateStore that stores a single state at each vertex, merging together all nondominated states.
 * @author mattwigway
 *
 */
public class SingleProfileStateStore implements ProfileStateStore {
    private Map<TransitStop, ProfileState> states = Maps.newHashMap();
    
    /**
     * we store the min upper bounds separately, because we also keep around the minimum upper bounds from previous rounds so we don't do unnecessary
     * searching.
     */
    private TObjectIntMap<TransitStop> minUpperBounds;
    
    @Override
    public boolean put(ProfileState ps) {
        if (ps.lowerBound >= minUpperBounds.get(ps.stop))
            return false;
        
        ps.previous = null;
        
        if (ps.upperBound < minUpperBounds.get(ps.stop))
            minUpperBounds.put(ps.stop, ps.upperBound);
        
        if (states.containsKey(ps.stop)) {
            // merge it in; it is not dominates
            ProfileState o = states.get(ps.stop);
            o.mergeIn(ps);
        }
        else {
            ps.patterns = null;
            states.put(ps.stop, ps);
        }
        
        return true;
    }

    @Override
    public Collection<ProfileState> get(final TransitStop tstop) {
        if (!states.containsKey(tstop))
            return Collections.EMPTY_LIST;
      
        return Collections.singletonList(states.get(tstop));
    }

    @Override
    public Collection<ProfileState> getAll() {
        return states.values();
    }
    
    @Override
    public int size() {
        return states.size();
    }
    
    public SingleProfileStateStore () {
        minUpperBounds = new TObjectIntHashMap<TransitStop>(5000, 0.75f, Integer.MAX_VALUE);
    }
    
    /**
     * initialize a single profile state store for a new round based on the minimum upper bounds from a previous round.
     * Note that the min upper bound object is not copied, so the other profile state store can no longer be added to.
     */
    public SingleProfileStateStore (SingleProfileStateStore other) {
        minUpperBounds = other.minUpperBounds;
    }

    @Override
    public Collection<TransitStop> keys() {
        return states.keySet();
    }

    @Override
    public boolean containsKey(TransitStop transitStop) {
        return states.containsKey(transitStop);
    }

}
