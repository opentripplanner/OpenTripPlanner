package org.opentripplanner.routing.core;

import java.util.HashMap;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.patch.NoteNarrative;
import org.opentripplanner.routing.patch.Patch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a wrapper around a new State that provides it with setter and increment methods,
 * allowing it to be modified before being put to use.
 * 
 * By virtue of being in the same package as States, it can modify their package private fields.
 * 
 * @author andrewbyrd
 */
public class StateEditor {

    private static final Logger _log = LoggerFactory.getLogger(StateEditor.class);

    protected State child;

    private boolean extensionsModified = false;

    private boolean spawned = false;

    private boolean defectiveTraversal = false;

    private boolean traversingBackward;

    /* CONSTRUCTORS */

    public StateEditor(State parent, Edge e) {
        this(parent, e, (EdgeNarrative) e);
    }

    public StateEditor(State parent, Edge e, EdgeNarrative en) {
        child = parent.clone();
        child.backState = parent;
        child.backEdge = e;
        child.backEdgeNarrative = en;
        child.hops = parent.hops + 1;
        // We clear child.next here, since it could have already been set in the parent
        child.next = null;
        // be clever
        // Note that we use equals(), not ==, here to allow for dynamically created vertices
        if (parent.vertex.equals(en.getFromVertex())) {
            traversingBackward = false;
            child.vertex = en.getToVertex();
        } else if (parent.vertex.equals(en.getToVertex())) {
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

    /**
     * Why can a state editor only be used once? If you modify some component of state with and
     * editor, use the editor to create a new state, and then make more modifications, these
     * modifications will be applied to the previously created state. Reusing the state editor to
     * make several states would modify an existing state somewhere earlier in the search, messing
     * up the shortest path tree.
     */
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
            _log.trace("Time was incremented the wrong direction during state editing. {}",
                    child.backEdge);
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

        return child.stateData.walkDistance >= options.maxWalkDistance;
    }

    public String toString() {
        return "<StateEditor " + child + ">";
    }

    /* PUBLIC METHODS TO MODIFY A STATE BEFORE IT IS USED */

    /**
     * Put a new value into the State extensions map. This will always clone the extensions map
     * before it is modified the first time, making sure that other references to the map in earlier
     * States are unaffected.
     */
    @SuppressWarnings("unchecked")
    public void setExtension(Object key, Object value) {
        cloneStateDataAsNeeded();
        if (!extensionsModified) {
            HashMap<Object, Object> newExtensions;
            if (child.stateData.extensions == null)
                newExtensions = new HashMap<Object, Object>(4);
            else
                newExtensions = (HashMap<Object, Object>) child.stateData.extensions.clone();
            child.stateData.extensions = newExtensions;
            extensionsModified = true;
        }
        child.stateData.extensions.put(key, value);
    }

    /**
     * Tell the stateEditor to return null when makeState() is called, no matter what other editing
     * has been done. This allows graph patches to block traversals.
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

    /* Incrementors */

    public void incrementWeight(double weight) {
        if (weight < 0) {
            _log.warn("A state's weight is being incremented by a negative amount while traversing edge "
                    + child.backEdge);
            defectiveTraversal = true;
            return;
        }
        child.weight += weight;
    }

    public void incrementTimeInSeconds(int seconds) {
        incrementTimeMsec(1000L * seconds);
    }

    /**
     * Advance or rewind the time of the new state by the given non-negative amount. Direction of
     * time is inferred from the direction of traversal. This is the only element of state that runs
     * backward when traversing backward.
     */
    public void incrementTimeMsec(long msec) {
        if (msec < 0) {
            _log.warn("A state's time is being incremented by a negative amount while traversing edge "
                    + child.getBackEdge());
            defectiveTraversal = true;
            return;
        }
        child.time += (traversingBackward ? -msec : msec);
    }

    public void incrementWalkDistance(double length) {
        cloneStateDataAsNeeded();
        if (length < 0) {
            _log.warn("A state's walk distance is being incremented by a negative amount.");
            defectiveTraversal = true;
            return;
        }
        child.stateData.walkDistance += length;
    }

    public void incrementNumBoardings() {
        cloneStateDataAsNeeded();
        child.stateData.numBoardings++;
    }

    /* Basic Setters */

    public void setTrip(int trip) {
        cloneStateDataAsNeeded();
        child.stateData.trip = trip;
    }

    public void setTripId(AgencyAndId tripId) {
        cloneStateDataAsNeeded();
        child.stateData.tripId = tripId;
    }

    public void setWalkDistance(double walkDistance) {
        cloneStateDataAsNeeded();
        child.stateData.walkDistance = walkDistance;
    }

    public void setZone(String zone) {
        cloneStateDataAsNeeded();
        child.stateData.zone = zone;
    }

    public void setRoute(AgencyAndId route) {
        cloneStateDataAsNeeded();
        child.stateData.route = route;
    }

    public void setNumBoardings(int numBoardings) {
        cloneStateDataAsNeeded();
        child.stateData.numBoardings = numBoardings;
    }

    public void setAlightedLocal(boolean alightedLocal) {
        cloneStateDataAsNeeded();
        child.stateData.alightedLocal = alightedLocal;
    }

    public void setEverBoarded(boolean everBoarded) {
        cloneStateDataAsNeeded();
        child.stateData.everBoarded = everBoarded;
    }

    public void setPreviousStop(Vertex previousStop) {
        cloneStateDataAsNeeded();
        child.stateData.previousStop = previousStop;
    }

    public void setLastAlightedTime(long lastAlightedTime) {
        cloneStateDataAsNeeded();
        child.stateData.lastAlightedTime = lastAlightedTime;
    }

    public void setTime(long t) {
        child.time = t;
    }

    public void setStartTime(long t) {
        cloneStateDataAsNeeded();
        child.stateData.startTime = t;
    }

    /**
     * Set non-incremental state values (ex. {@link State#getRoute()}) from an existing state.
     * Incremental values (ex. {@link State#getNumBoardings()}) are not currently set.
     * 
     * @param state
     */
    public void setFromState(State state) {
        cloneStateDataAsNeeded();
        child.stateData.route = state.stateData.route;
        child.stateData.trip = state.stateData.trip;
        child.stateData.tripId = state.stateData.tripId;
        child.stateData.zone = state.stateData.zone;
    }

    /* PUBLIC GETTER METHODS */

    /*
     * Allow patches to see the State being edited, so they can decide whether to apply their
     * transformations to the traversal result or not.
     */

    public Object getExtension(Object key) {
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
     * Find any patches that have been applied to the edge being traversed (i.e. the new child
     * state's back edge) and allow these patches to manipulate the StateEditor before the child
     * state is put to use.
     * 
     * @return whether any patches were applied
     */
    private boolean applyPatches() {
        boolean filtered = false;
        List<Patch> patches = child.backEdge.getPatches();
        if (patches != null) {
            for (Patch patch : patches) {
                if (!patch.activeDuring(child.stateData.options, child.getStartTime(),
                        child.getTime())) {
                    continue;
                }
                patch.filterTraverseResult(this);
                filtered = true;
            }
        }
        return filtered;
    }

    /**
     * To be called before modifying anything in the child's StateData. Makes sure that changes are
     * applied to a copy of StateData rather than the same one that is still referenced in existing,
     * older states.
     */
    private void cloneStateDataAsNeeded() {
        if (child.stateData == child.backState.stateData)
            child.stateData = child.stateData.clone();
    }
}
