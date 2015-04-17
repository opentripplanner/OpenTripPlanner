package org.opentripplanner.routing.algorithm;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;

import java.util.Collection;
import java.util.Iterator;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.vertextype.TransitStop;

/** Stores the states used in RAPTOR; allows easily switching to path-reconstructing store, a highly efficient optimal store, or McRAPTOR */
public interface RaptorStateStore {
    /** Add a state to this store, with the given clock time (seconds since midnight) */
    public boolean put (TransitStop stop, int clockTime, boolean transfer);
    
    /** Proceed to the next round */
    public void proceed ();
    
    /** get the best time for a given stop that was reached by transit (not by transfers) */
    public int getTime(TransitStop t);
    
    /** get the best time for a given stop after the last round, including transfers */
    public int getPrev(TransitStop t);
    
    /** get an iterator over the states of this store */
    public TObjectIntIterator<TransitStop> iterator ();
    
    /** get all the stops that were reached in the current round, either via transfers or directly */
    public Collection<TransitStop> getTouchedStopsIncludingTransfers();
}
