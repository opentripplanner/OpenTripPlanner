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
    /** stores what is reachable in the current round. This stores elapsed time not clock time, but elapsed time is just clock time minus constant start time. */
    private TObjectIntMap<Vertex> current = new TObjectIntHashMap<Vertex>(1000, 0.75f, Integer.MAX_VALUE);
    
    /** stores what was reachable in the previous round */
    private TObjectIntMap<Vertex> prev;
    
    private RoutingRequest options;
    
    @Override
    public boolean put(State s) {
        Vertex v = s.getVertex();
        
        if (!(v instanceof TransitStop))
            throw new UnsupportedOperationException("Vertex is not a transit stop!");
        
        TransitStop t = (TransitStop) v;
        
        int elapsedTime = (int) s.getElapsedTimeSeconds();
        if (elapsedTime < current.get(t)) {
            current.put(t, elapsedTime);
            return true;
        }
        return false;
    }

    private State get(TransitStop t, TObjectIntMap which) {
        if (!which.containsKey(t))
            return null;
        
        int elapsedTimeSecs = which.get(t);
        long startTime = options.dateTime;
        return new State(t, null, elapsedTimeSecs + startTime, startTime, options);
    }
    
    

    @Override
    public void proceed() {
        prev = new TObjectIntHashMap<Vertex>();
        prev.putAll(current);
    }

    @Override
    public State getCurrent(TransitStop t) {
        return get(t, current);
    }

    @Override
    public State getPrev(TransitStop t) {
        return get(t, prev);
    }
    
    public PathDiscardingRaptorStateStore(RoutingRequest options) {
        this.options = options;
    }
    
    private Iterator<State> iterator (final TObjectIntMap<Vertex> map) {
        return new Iterator<State> () {
            private TObjectIntIterator<Vertex> baseIterator = map.iterator();
            
            @Override
            public boolean hasNext() {
                return baseIterator.hasNext();
            }

            @Override
            public State next() {
                baseIterator.advance();
                long startTime = options.dateTime;
                return new State(baseIterator.key(), null, baseIterator.value() + startTime, startTime, options);
            }

            @Override
            public void remove() {
                baseIterator.remove();
            }
            
        };
    }
    
    public Iterator<State> currentIterator() {
        return iterator(current);
    }
    
    public Iterator<State> prevIterator() {
        return iterator(prev);
    }

}
