/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import java.util.HashMap;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.patch.NoteNarrative;
import org.opentripplanner.routing.patch.Alert;
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
        // We clear child.next here, since it could have already been set in the
        // parent
        child.next = null;
        if (e == null) {
            child.backState = null;
            child.hops = 0;
            child.vertex = parent.vertex;
            child.stateData = child.stateData.clone();
        } else {
            child.hops = parent.hops + 1;
            // be clever
            // Note that we use equals(), not ==, here to allow for dynamically
            // created vertices
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
                _log
                        .error("Actual traversal direction does not match traversal direction in TraverseOptions.");
                defectiveTraversal = true;
            }
            if (parent.stateData.noThruTrafficState == NoThruTrafficState.INIT && !(e instanceof FreeEdge)) {
                setNoThruTrafficState(NoThruTrafficState.BETWEEN_ISLANDS);
            }
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

        if (child.backState != null) {
            // make it impossible to use a state with lower weight than its
            // parent.
            child.checkNegativeWeight();

            // check that time changes are coherent with edge traversal
            // direction
            if (traversingBackward ? (child.getTimeDeltaSec() > 0)
                    : (child.getTimeDeltaSec() < 0)) {
                _log.trace("Time was incremented the wrong direction during state editing. {}",
                        child.backEdge);
                return null;
            }

            if(!applyPatches()) {
                return null;
            }
        }
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
    public void addAlert(Alert notes) {
        child.backEdgeNarrative = new NoteNarrative(child.backEdgeNarrative, notes);
    }

    /* Incrementors */

    public void incrementWeight(double weight) {
        if (Double.isNaN(weight)) {
            _log.warn("A state's weight is being incremented by NaN while traversing edge "
                    + child.backEdge);
            defectiveTraversal = true;
            return;
        }
        if (weight < 0) {
            _log.warn("A state's weight is being incremented by a negative amount while traversing edge "
                    + child.backEdge);
            defectiveTraversal = true;
            return;
        }
        child.weight += weight;
    }


    /**
     * Advance or rewind the time of the new state by the given non-negative amount. Direction of
     * time is inferred from the direction of traversal. This is the only element of state that runs
     * backward when traversing backward.
     */
    public void incrementTimeInSeconds(int seconds) {
        if (seconds < 0) {
            _log.warn("A state's time is being incremented by a negative amount while traversing edge "
                    + child.getBackEdge());
            defectiveTraversal = true;
            return;
        }
        child.time += (traversingBackward ? -seconds : seconds);
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
        final int SMALL_PRIME = 31;
        final int LARGE_PRIME = 24421;
        cloneStateDataAsNeeded();
        child.stateData.tripId = tripId;
        // tripId is set to null when alighting.
        // only update hash when encountering a new tripid
        if (tripId != null) {
            child.stateData.tripSeqHash *= LARGE_PRIME;
            child.stateData.tripSeqHash += tripId.getId().hashCode() * SMALL_PRIME;
        }
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

    public void setNoThruTrafficState(NoThruTrafficState noThruTrafficState) {
        cloneStateDataAsNeeded();
        child.stateData.noThruTrafficState = noThruTrafficState;
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
        child.stateData.extensions = state.stateData.extensions;
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

    public NoThruTrafficState getNoThruTrafficState() {
        return child.stateData.noThruTrafficState;
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
     * @return false if a patch blocked traversal
     */
    private boolean applyPatches() {
        List<Patch> patches = child.backEdge.getPatches();
        boolean display = false, active = false;

        if (patches != null) {
            for (Patch patch : patches) {
                active  = false;
                display = patch.displayDuring(child.stateData.options, child.getStartTime(),
                                              child.getTime());

                if(!display) {
                    active = patch.activeDuring(child.stateData.options, child.getStartTime(),
                                                child.getTime());
                }

                if(display || active) {
                    if(!patch.filterTraverseResult(this, display))
                        return false;
            }
        }
        }

        return true;
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

    public void setTraverseOptions(TraverseOptions options) {
        child.stateData.options = options;
        traversingBackward = options.isArriveBy();
    }

    public void alightTransit() {
        child.setWalkAtLastTransit(child.getWalkDistance());
    }
}
