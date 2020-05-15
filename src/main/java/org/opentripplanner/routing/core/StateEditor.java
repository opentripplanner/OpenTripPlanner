package org.opentripplanner.routing.core;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * This class is a wrapper around a new State that provides it with setter and increment methods,
 * allowing it to be modified before being put to use.
 * 
 * By virtue of being in the same package as States, it can modify their package private fields.
 * 
 * @author andrewbyrd
 */
public class StateEditor {

    private static final Logger LOG = LoggerFactory.getLogger(StateEditor.class);

    protected State child;

    private boolean spawned = false;

    private boolean defectiveTraversal = false;

    private boolean traversingBackward;

    /* CONSTRUCTORS */

    protected StateEditor() {}
    
    public StateEditor(RoutingRequest options, Vertex v) {
        child = new State(v, options);
    }

    public StateEditor(State parent, Edge e) {
        child = parent.clone();
        child.backState = parent;
        child.backEdge = e;
        // We clear child.next here, since it could have already been set in the
        // parent
        child.next = null;
        if (e == null) {
            child.backState = null;
            child.vertex = parent.vertex;
            child.stateData = child.stateData.clone();
        } else {
            // be clever
            // Note that we use equals(), not ==, here to allow for dynamically
            // created vertices
            if (e.getFromVertex().equals(e.getToVertex())
                    && parent.vertex.equals(e.getFromVertex())) {
                // TODO LG: We disable this test: the assumption that
                // the from and to vertex of an edge are not the same
                // is not true anymore: bike rental on/off edges.
                traversingBackward = parent.getOptions().arriveBy;
                child.vertex = e.getToVertex();
            } else if (parent.vertex.equals(e.getFromVertex())) {
                traversingBackward = false;
                child.vertex = e.getToVertex();
            } else if (parent.vertex.equals(e.getToVertex())) {
                traversingBackward = true;
                child.vertex = e.getFromVertex();
            } else {
                // Parent state is not at either end of edge.
                LOG.warn("Edge is not connected to parent state: {}", e);
                LOG.warn("   from   vertex: {}", e.getFromVertex());
                LOG.warn("   to     vertex: {}", e.getToVertex());
                LOG.warn("   parent vertex: {}", parent.vertex);
                defectiveTraversal = true;
            }
            if (traversingBackward != parent.getOptions().arriveBy) {
                LOG.error("Actual traversal direction does not match traversal direction in TraverseOptions.");
                defectiveTraversal = true;
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
            LOG.error("Defective traversal flagged on edge " + child.backEdge);
            return null;
        }

        if (child.backState != null) {
            // make it impossible to use a state with lower weight than its
            // parent.
            child.checkNegativeWeight();

            // check that time changes are coherent with edge traversal
            // direction
            if (traversingBackward ? (child.getTimeDeltaSeconds() > 0)
                    : (child.getTimeDeltaSeconds() < 0)) {
                LOG.trace("Time was incremented the wrong direction during state editing. {}",
                        child.backEdge);
                return null;
            }
        }
        spawned = true;
        return child;
    }

    public String toString() {
        return "<StateEditor " + child + ">";
    }

    /* PUBLIC METHODS TO MODIFY A STATE BEFORE IT IS USED */

    /**
     * Tell the stateEditor to return null when makeState() is called, no matter what other editing
     * has been done. This allows graph patches to block traversals.
     */
    public void blockTraversal() {
        this.defectiveTraversal = true;
    }

    /* Incrementors */

    public void incrementWeight(double weight) {
        if (Double.isNaN(weight)) {
            LOG.warn("A state's weight is being incremented by NaN while traversing edge "
                    + child.backEdge);
            defectiveTraversal = true;
            return;
        }
        if (weight < 0) {
            LOG.warn("A state's weight is being incremented by a negative amount while traversing edge "
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
        incrementTimeInMilliseconds(seconds * 1000L);
    }
    
    public void incrementTimeInMilliseconds(long milliseconds) {
        if (milliseconds < 0) {
            LOG.warn("A state's time is being incremented by a negative amount while traversing edge "
                    + child.getBackEdge());
            defectiveTraversal = true;
            return;
        }
        child.time += (traversingBackward ? -milliseconds : milliseconds);
    }    

    public void incrementWalkDistance(double length) {
        if (length < 0) {
            LOG.warn("A state's walk distance is being incremented by a negative amount.");
            defectiveTraversal = true;
            return;
        }
        child.walkDistance += length;
    }

    /* Basic Setters */

    public void setEnteredNoThroughTrafficArea() {
        child.stateData.enteredNoThroughTrafficArea = true;
    }
    

    public void setBackMode(TraverseMode mode) {
        if (mode == child.stateData.backMode)
            return;
        
        cloneStateDataAsNeeded();
        child.stateData.backMode = mode;
    }

    public void setBackWalkingBike (boolean walkingBike) {
        if (walkingBike == child.stateData.backWalkingBike)
            return;
        
        cloneStateDataAsNeeded();
        child.stateData.backWalkingBike = walkingBike;
    }

    public void setWalkDistance(double walkDistance) {
        child.walkDistance = walkDistance;
    }

    public void beginVehicleRenting(TraverseMode vehicleMode) {
        cloneStateDataAsNeeded();
        child.stateData.usingRentedBike = true;
        child.stateData.hasUsedRentedBike = true;
        child.stateData.nonTransitMode = vehicleMode;
    }

    public void doneVehicleRenting() {
        cloneStateDataAsNeeded();
        child.stateData.usingRentedBike = false;
        child.stateData.nonTransitMode = TraverseMode.WALK;
    }

    /**
     * This has two effects: marks the car as parked, and switches the current mode.
     * Marking the car parked is important for allowing co-dominance of walking and driving states.
     */
    public void setCarParked(boolean carParked) {
        cloneStateDataAsNeeded();
        child.stateData.carParked = carParked;
        if (carParked) {
            // We do not handle mixed-mode P+BIKE...
            child.stateData.nonTransitMode = TraverseMode.WALK;
        } else {
            child.stateData.nonTransitMode = TraverseMode.CAR;
        }
    }

    public void setBikeParked(boolean bikeParked) {
        cloneStateDataAsNeeded();
        child.stateData.bikeParked = bikeParked;
        if (bikeParked) {
            child.stateData.nonTransitMode = TraverseMode.WALK;
        } else {
            child.stateData.nonTransitMode = TraverseMode.BICYCLE;
        }
    }

    public void setTimeSeconds(long seconds) {
        child.time = seconds * 1000;
    }

    public void setStartTimeSeconds(long seconds) {
        cloneStateDataAsNeeded();
        child.stateData.startTime = seconds;
    }

    /**
     * Set non-incremental state values from an existing state.
     * Incremental values are not currently set.
     * 
     * @param state
     */
    public void setFromState(State state) {
        cloneStateDataAsNeeded();
        child.stateData.usingRentedBike = state.stateData.usingRentedBike;
        child.stateData.carParked = state.stateData.carParked;
        child.stateData.bikeParked = state.stateData.bikeParked;
    }

    public void setNonTransitOptionsFromState(State state){
        cloneStateDataAsNeeded();
        child.stateData.nonTransitMode = state.getNonTransitMode();
        child.stateData.carParked = state.isCarParked();
        child.stateData.bikeParked = state.isBikeParked();
        child.stateData.usingRentedBike = state.isBikeRenting();
    }

    public void setTaxiState(CarPickupState carPickupState) {
        child.stateData.carPickupState = carPickupState;
        switch (carPickupState) {
            case WALK_TO_PICKUP:
            case WALK_FROM_DROP_OFF:
                child.stateData.nonTransitMode = TraverseMode.WALK;
                break;
            case IN_CAR:
                child.stateData.nonTransitMode = TraverseMode.CAR;
                break;
        }
    }

    /* PUBLIC GETTER METHODS */

    public long getTimeSeconds() {
        return child.getTimeSeconds();
    }

    public long getElapsedTimeSeconds() {
        return child.getElapsedTimeSeconds();
    }

    public boolean isRentingBike() {
        return child.isBikeRenting();
    }

    public double getWalkDistance() {
        return child.getWalkDistance();
    }

    public Vertex getVertex() {
        return child.getVertex();
    }

    /* PRIVATE METHODS */

    /**
     * To be called before modifying anything in the child's StateData. Makes sure that changes are
     * applied to a copy of StateData rather than the same one that is still referenced in existing,
     * older states.
     */
    private void cloneStateDataAsNeeded() {
        if (child.backState != null && child.stateData == child.backState.stateData)
            child.stateData = child.stateData.clone();
    }

    public void setOptions(RoutingRequest options) {
        cloneStateDataAsNeeded();
        child.stateData.opt = options;
    }

    public void setBikeRentalNetwork(Set<String> networks) {
        cloneStateDataAsNeeded();
        child.stateData.bikeRentalNetworks = networks;
    }

    public boolean hasEnteredNoThroughTrafficArea() {
        return child.hasEnteredNoThruTrafficArea();
    }

}
