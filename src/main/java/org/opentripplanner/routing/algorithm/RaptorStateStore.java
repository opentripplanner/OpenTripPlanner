package org.opentripplanner.routing.algorithm;

import gnu.trove.iterator.TObjectIntIterator;

import java.util.Iterator;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.vertextype.TransitStop;

/** Stores the states used in RAPTOR; allows easily switching to path-reconstructing store, a highly efficient optimal store, or McRAPTOR */
public interface RaptorStateStore {
    /** Add a state to this store, with the given clock time (seconds since midnight) */
    public boolean put (TransitStop stop, int clockTime);
    
    /** Get a clock time in seconds since midnight from this store after the current round */
    public int getCurrent (TransitStop t);
    
    /** Get a clock time in seconds since midnight from this store after the previous round */
    public int getPrev (TransitStop t);
    
    /** Proceed to the next round */
    public void proceed ();
    
    /** Iterator over all current states */
    public TObjectIntIterator<TransitStop> currentIterator();
    
    /** Iterator over all states from the previous round */
    public TObjectIntIterator<TransitStop> prevIterator();
}
