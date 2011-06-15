package org.opentripplanner.routing.core;

import java.util.HashMap;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.patch.NoteNarrative;
import org.opentripplanner.routing.patch.Patch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a wrapper around a new State that provides it with
 * setter and increment methods, allowing it to be modified before
 * being put to use.
 * 
 * By virtue of being in the same package as States, it can modify
 * their package private fields.
 * 
 * @author andrewbyrd
 */
public class StateEditor {
    private static final Logger _log = LoggerFactory.getLogger(StateEditor.class);

	private State child;
	private boolean extensionsModified = false;
	private boolean spawned = false;
	private boolean defectiveTraversal = false;
	private boolean traversingBackward;

	/* CONSTRUCTORS */ 

	public StateEditor (State parent, Edge e) {
		this(parent, e, (EdgeNarrative)e);
	}

	public StateEditor (State parent, Edge e, EdgeNarrative en) {
		child = parent.clone();
        child.backState = parent;
        child.backEdge = e;
        child.backEdgeNarrative = en;
        child.hops = parent.hops + 1;
        // be clever
        if (parent.vertex == en.getFromVertex()) {
        	traversingBackward = false; 
        	child.vertex = en.getToVertex();
        } else if (parent.vertex == en.getToVertex()) {
        	traversingBackward = true;
        	child.vertex = en.getFromVertex();
        } else {
			// Parent state is not at either end of edge. 
			_log.warn("Edge is not connected to parent state: {}", en);
			_log.warn("   from   vertex: {}", en.getFromVertex());
			_log.warn("   to     vertex: {}", en.getToVertex());
			_log.warn("   parent vertex: {}", parent.vertex);
			defectiveTraversal = true;
        }
        if (traversingBackward != parent.getOptions().isArriveBy()) {
        	_log.error("Actual traversal direction does not match traversal direction in TraverseOptions.");
        	defectiveTraversal = true;
        }
	}
	
	/* PUBLIC METHODS */
	
	public State makeState() {
		// check that this editor has not been used already
		if (spawned) 
			throw new IllegalStateException("A StateEditor can only be used once.");
		
		// if something was flagged incorrect, do not make a new state
		if (defectiveTraversal) {
        	_log.error("Defective traversal flagged on edge " + child.backEdge);
			return null;
		}
		
		// make it impossible to use a state with lower weight than its parent.
		child.checkNegativeWeight();
		
		// check that time changes are coherent with edge traversal direction
		if (traversingBackward ? (child.getTimeDeltaMsec() > 0) : (child.getTimeDeltaMsec() < 0)) {
			_log.trace("Time was incremented the wrong direction during state editing. {}", child.backEdge);
			return null;
		}
		
		applyPatches();
		spawned = true;
		return child;
	}

	public boolean weHaveWalkedTooFar(TraverseOptions options) {
        // Only apply limit in transit-only case
        if (!options.getModes().getTransit())
            return false;

        // A maxWalkDistance of 0 or less indicates no limit
        if (options.maxWalkDistance <= 0)
            return false;

        return child.walkDistance >= options.maxWalkDistance;
    }
	
	public String toString() {
        return "<StateEditor " + child + ">";
    }
    


	/* PUBLIC METHODS TO MODIFY A STATE BEFORE IT IS USED */

    /**
     * Put a new value into the State extensions map.
     * This will always clone the extensions map before it is modified the
     * first time, making sure that other references to the map in 
     * earlier States are unaffected.
     * 
     * @param key
     * @param value
     */
	@SuppressWarnings("unchecked")
	public void setExtension (Object key, Object value) {
    	if (!extensionsModified) {
        	HashMap<Object, Object> newExtensions;
	    	if (child.extensions == null) newExtensions = new HashMap(4);
	    	else newExtensions = (HashMap<Object, Object>) child.extensions.clone();
	    	child.extensions = newExtensions;
	    	extensionsModified = true;
    	}
    	child.extensions.put(key, value);
    }

	/**
	 * Tell the stateEditor to return null when makeState() is called, no matter
	 * what other editing has been done. This allows graph patches to block traversals. 
	 */
	public void blockTraversal() {
		this.defectiveTraversal = true;
	}
	
	/**
	 * Wrap the new State's predecessor EdgeNarrative so that it has the given note.
	 */
	public void addNote(String note) {
		child.backEdgeNarrative = new NoteNarrative(child.backEdgeNarrative, note);
	}
	
	/* Convenience methods to modify several fields at once */

	public void walk(double distance, TraverseOptions options) {
		double walkTime = distance / options.speed; // meters per second
		child.walkDistance += distance;
		child.time += walkTime;
		child.weight += walkTime * options.walkReluctance;
	}
	
	public void wait(int seconds, TraverseOptions options) {
		child.time += seconds * 1000;
		child.weight += seconds * options.waitReluctance;
	}

	public void ride(int seconds, TraverseOptions options) {
		child.time += seconds * 1000;
		child.weight += seconds; // by definition 1 second IVT is unity
	}

	public void board(TraverseOptions options) {
        // Handle zone, route, farecontext, trip, tripid here?
		child.numBoardings += 1;
        child.everBoarded = true; // isn't this the same as numBoardings > 0 ?
        child.weight += options.boardCost;
	}
	
	/* Incrementors */
	
	public void incrementWeight(double weight) {
		if (weight < 0) {
			_log.warn("A state's weight is being incremented by a negative amount while traversing edge " + child.backEdge);
			defectiveTraversal = true;
			return;
		}
		child.weight += weight;
	}
	
	public void incrementTimeInSeconds(int seconds) {
		incrementTimeMsec(1000L * seconds);
	}
	
	/**
	 * Advance or rewind the time of the new state by the given non-negative amount. 
	 * Direction of time is inferred from the direction of traversal.
	 * This is the only element of state that runs backward when traversing backward.
	 */
	public void incrementTimeMsec(long msec) {
		if (msec < 0) {
			_log.warn("A state's time is being incremented by a negative amount while traversing edge " + child.getBackEdge());
			defectiveTraversal = true;
			return;
		}
		child.time += (traversingBackward ? - msec : msec);		
	}

	public void incrementWalkDistance(double length) {
		if (length < 0) {
			_log.warn("A state's walk distance is being incremented by a negative amount.");
			defectiveTraversal = true;
			return;
		}
		child.walkDistance += length;
	}

    public void incrementNumBoardings() {
        child.numBoardings++;
    }

    /* Basic Setters */
    
    public void setTrip(int trip) {
        child.trip = trip;
    }

    public void setTripId(AgencyAndId tripId) {
        child.tripId = tripId;
    }

    public void setWalkDistance(double walkDistance) {
        child.walkDistance = walkDistance;
    }

    public void setZone(String zone) {
        child.zone = zone;
    }

    public void setRoute(AgencyAndId route) {
        child.route = route;
    }

    public void setFareContext(FareContext fareContext) {
        child.fareContext = fareContext;
    }

    public void setNumBoardings(int numBoardings) {
        child.numBoardings = numBoardings;
    }

    public void setAlightedLocal(boolean alightedLocal) {
        child.alightedLocal = alightedLocal;
    }

    public void setEverBoarded(boolean everBoarded) {
        child.everBoarded = everBoarded;
    }

    public void setPreviousStop(Vertex previousStop) {
        child.previousStop = previousStop;
    }

    public void setLastAlightedTime(long lastAlightedTime) {
        child.lastAlightedTime = lastAlightedTime;
    }

	public void setTime(long t) {
		child.time = t;
	}

	/* PUBLIC GETTER METHODS */
	
	/* Allow patches to see the State being edited, so they can decide whether to
	 * apply their transformations to the traversal result or not.
	 */

	public Object getExtension (Object key) { 
    	return child.getExtension(key); 
    }
    
    public long getTime() {
    	return child.getTime();
    }
    
    public long getElapsedTime() {
    	return child.getElapsedTime();
    }

    public int getTrip() {
        return child.getTrip();
    }

    public AgencyAndId getTripId() {
        return child.getTripId();
    }

    public String getZone() {
        return child.getZone();
    }

    public AgencyAndId getRoute() {
        return child.getRoute();
    }

    public FareContext getFareContext() {
        return child.getFareContext();
    }

    public int getNumBoardings() {
        return child.getNumBoardings();
    }

    public boolean isAlightedLocal() {
        return child.isAlightedLocal();
    }

    public boolean isEverBoarded() {
        return child.isEverBoarded();
    }

    public Vertex getPreviousStop() {
        return child.getPreviousStop();
    }

    public long getLastAlightedTime() {
        return child.getLastAlightedTime();
    }

    public double getWalkDistance() {
        return child.getWalkDistance();
    }
    
	public Vertex getVertex() {
		return child.getVertex();
	}
	
	/* PRIVATE METHODS */
	
	/**
	 * Find any patches that have been applied to the edge being traversed (i.e. the
	 * new child state's back edge) and allow these patches to manipulate the 
	 * StateEditor before the child state is put to use.
	 * 
	 * @return whether any patches were applied
	 */
	private boolean applyPatches() {
		boolean filtered = false;
        List<Patch> patches = child.backEdge.getPatches();
        if (patches != null) {
        	for (Patch patch : patches) {
            	if (!patch.activeDuring(child.options, child.getStartTime(), child.getTime())) {
            		continue;
            	}
               	patch.filterTraverseResult(this);
               	filtered = true;
        	}
        }
        return filtered;
	}

}
