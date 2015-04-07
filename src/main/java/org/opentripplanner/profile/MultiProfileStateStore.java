package org.opentripplanner.profile;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.opentripplanner.routing.vertextype.TransitStop;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class MultiProfileStateStore implements ProfileStateStore {
    private Multimap<TransitStop, ProfileState> states = ArrayListMultimap.create();
    
    /** cache the minimum upper bounds for each stop, for performance */
    private TObjectIntMap<TransitStop> minUpperBounds;
    
    @Override
    public boolean put(ProfileState ps) {
        if (ps.lowerBound < minUpperBounds.get(ps.stop)) {
            states.put(ps.stop, ps);
            
            if (ps.upperBound < minUpperBounds.get(ps.stop)) {
                minUpperBounds.put(ps.stop, ps.upperBound);
                
                // kick out old states
                for (Iterator<ProfileState> it = states.get(ps.stop).iterator(); it.hasNext();) {
                    // need to use strictly-greater-than here, so states with no variation don't dominate themselves.
                    if (it.next().lowerBound > ps.upperBound) {
                        it.remove();
                    }
                }
            }
            
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public Collection<ProfileState> get(TransitStop tstop) {
        return states.get(tstop);
    }

    @Override
    public Collection<ProfileState> getAll() {
        return states.values();
    }
    
    @Override
    public int size() {
        return states.size();
    }
    
    /** merge similar states (states that have come from the same place on different patterns) */
    public void mergeStates() {
        Set<TransitStop> touchedStopVertices = new HashSet<TransitStop>(states.keySet());
        for (TransitStop tstop : touchedStopVertices) {
            Collection<ProfileState> pss = states.get(tstop);
            
            // find states that have come from the same place
            Multimap<ProfileState, ProfileState> foundStates = ArrayListMultimap.create();
            
            for (Iterator<ProfileState> it = pss.iterator(); it.hasNext();) {
                ProfileState ps = it.next();
                foundStates.put(ps.previous, ps);
            }
            
            pss.clear();
            
            // merge them now
            for (Collection<ProfileState> states : foundStates.asMap().values()) {                   
                if (states.size() == 1)
                    pss.addAll(states);
                else
                    pss.add(ProfileState.merge(states, true));
            }
        }
    }

    /**
     * initialize a multi profile state store for a new round based on the minimum upper bounds from a previous round.
     * Note that the min upper bound object is not copied, so the other profile state store can no longer be added to.
     */
    public MultiProfileStateStore (MultiProfileStateStore other) {
        minUpperBounds = other.minUpperBounds;
    }
    
    public MultiProfileStateStore () {
        minUpperBounds = new TObjectIntHashMap<TransitStop>(5000, 0.75f, Integer.MAX_VALUE);
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
