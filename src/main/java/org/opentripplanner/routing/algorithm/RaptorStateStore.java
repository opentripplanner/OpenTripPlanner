package org.opentripplanner.routing.algorithm;

import java.util.Iterator;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.vertextype.TransitStop;

/** Stores the states used in RAPTOR; allows easily switching to path-reconstructing store, a highly efficient optimal store, or McRAPTOR */
public interface RaptorStateStore {
    /** Add a state to this store */
    public boolean put (State s);
    
    /** Get a state from this store after the current round */
    public State getCurrent (TransitStop t);
    
    /** Get a state from this store after the previous round */
    public State getPrev (TransitStop t);
    
    /** Proceed to the next round */
    public void proceed ();
    
    /** Iterator over all current states */
    public Iterator<State> currentIterator();
    
    /** Iterator over all states from the previous round */
    public Iterator<State> prevIterator();
}
