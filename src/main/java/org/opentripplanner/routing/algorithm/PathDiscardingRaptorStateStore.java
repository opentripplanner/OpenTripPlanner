package org.opentripplanner.routing.algorithm;

import java.util.Iterator;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class PathDiscardingRaptorStateStore implements RaptorStateStore {
    /** stores what is reachable in the current round. This stores clock time in seconds since midnight.  */
    private TObjectIntMap<TransitStop> current = new TObjectIntHashMap<TransitStop>(1000, 0.75f, Integer.MAX_VALUE);
    
    /** stores what was reachable in the previous round */
    private TObjectIntMap<TransitStop> prev;
    
    @Override
    public boolean put(TransitStop t, int time) {
        if (time < current.get(t)) {
            current.put(t, time);
            return true;
        }
        return false;
    }    

    @Override
    public void proceed() {
        prev = new TObjectIntHashMap<TransitStop>(1000, 0.75f, Integer.MAX_VALUE);
        prev.putAll(current);
    }

    @Override
    public int getCurrent(TransitStop t) {
        return current.get(t);
    }

    @Override
    public int getPrev(TransitStop t) {
        return prev.get(t);
    }
    
    public TObjectIntIterator<TransitStop> currentIterator() {
        return current.iterator();
    }
    
    public TObjectIntIterator<TransitStop> prevIterator() {
        return prev.iterator();
    }

}
