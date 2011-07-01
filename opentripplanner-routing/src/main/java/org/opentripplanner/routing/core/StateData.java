package org.opentripplanner.routing.core;

import java.util.HashMap;

import org.onebusaway.gtfs.model.AgencyAndId;

/**
 * StateData contains the components of search state that are unlikely to be changed as often
 * as time or weight. This avoids frequent duplication, which should have a positive
 * impact on both time and space use during searches.
 */
public class StateData implements Cloneable {
	
	// the time at which the search started
    protected long startTime;
    // which trip index inside a pattern
    protected int trip;
    protected AgencyAndId tripId;
    // how far have we walked
    protected double walkDistance;
    protected String zone;
    protected AgencyAndId route;
    protected int numBoardings;
    protected boolean alightedLocal;
    protected boolean everBoarded;
    protected Vertex previousStop;
    protected long lastAlightedTime;
    protected HashMap<Object, Object> extensions;
    // the traverseOptions that were used to reach this state
    protected TraverseOptions options;

    protected StateData clone() {
    	try {
			return (StateData) super.clone();
		} catch (CloneNotSupportedException e1) {
			throw new IllegalStateException("This is not happening");
		}
	}

}
