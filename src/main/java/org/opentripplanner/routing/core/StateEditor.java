package org.opentripplanner.routing.core;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a wrapper around a new State that provides it with setter and increment methods,
 * allowing it to be modified before being put to use.
 * <p>
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

    protected StateEditor() {
    }

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
        } else if (e.getFromVertex() == null || e.getToVertex() == null) {
            child.vertex = parent.vertex;
            child.stateData = child.stateData.clone();
            LOG.error("From or to vertex is null for {}", e);
            defectiveTraversal = true;
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

    public void resetEnteredNoThroughTrafficArea() {
        if (!child.stateData.enteredNoThroughTrafficArea) {
            return;
        }

        cloneStateDataAsNeeded();
        child.stateData.enteredNoThroughTrafficArea = false;
    }

    public void setEnteredNoThroughTrafficArea() {
        if (child.stateData.enteredNoThroughTrafficArea) {
            return;
        }

        cloneStateDataAsNeeded();
        child.stateData.enteredNoThroughTrafficArea = true;
    }

    public void setBackMode(TraverseMode mode) {
        if (mode == child.stateData.backMode)
            return;

        cloneStateDataAsNeeded();
        child.stateData.backMode = mode;
    }

    public void setBackWalkingBike(boolean walkingBike) {
        if (walkingBike == child.stateData.backWalkingBike)
            return;

        cloneStateDataAsNeeded();
        child.stateData.backWalkingBike = walkingBike;
    }

    public void setWalkDistance(double walkDistance) {
        child.walkDistance = walkDistance;
    }

    public void beginFloatingVehicleRenting(
            FormFactor formFactor,
            String network,
            boolean reverse
    ) {
        cloneStateDataAsNeeded();
        if (reverse) {
            child.stateData.vehicleRentalState = VehicleRentalState.BEFORE_RENTING;
            child.stateData.currentMode = TraverseMode.WALK;
            child.stateData.vehicleRentalNetwork = null;
            child.stateData.rentalVehicleFormFactor = null;
        } else {
            child.stateData.vehicleRentalState = VehicleRentalState.RENTING_FLOATING;
            child.stateData.currentMode = formFactor.traverseMode;
            child.stateData.vehicleRentalNetwork = network;
            child.stateData.rentalVehicleFormFactor = formFactor;
        }
    }

    public void beginVehicleRentingAtStation(
            FormFactor formFactor,
            String network,
            boolean mayKeep,
            boolean reverse
    ) {
        cloneStateDataAsNeeded();
        if (reverse) {
            child.stateData.mayKeepRentedVehicleAtDestination = mayKeep;
            child.stateData.vehicleRentalState = VehicleRentalState.BEFORE_RENTING;
            child.stateData.currentMode = TraverseMode.WALK;
            child.stateData.vehicleRentalNetwork = null;
            child.stateData.rentalVehicleFormFactor = null;
            child.stateData.backWalkingBike = false;
        } else {
            child.stateData.mayKeepRentedVehicleAtDestination = mayKeep;
            child.stateData.vehicleRentalState = VehicleRentalState.RENTING_FROM_STATION;
            child.stateData.currentMode = formFactor.traverseMode;
            child.stateData.vehicleRentalNetwork = network;
            child.stateData.rentalVehicleFormFactor = formFactor;
        }
    }

    public void dropOffRentedVehicleAtStation(
            FormFactor formFactor,
            String network,
            boolean reverse
    ) {
        cloneStateDataAsNeeded();
        if (reverse) {
            child.stateData.mayKeepRentedVehicleAtDestination = false;
            child.stateData.vehicleRentalState = VehicleRentalState.RENTING_FROM_STATION;
            child.stateData.currentMode = formFactor.traverseMode;
            child.stateData.vehicleRentalNetwork = network;
            child.stateData.rentalVehicleFormFactor = formFactor;
        } else {
            child.stateData.mayKeepRentedVehicleAtDestination = false;
            child.stateData.vehicleRentalState = VehicleRentalState.HAVE_RENTED;
            child.stateData.currentMode = TraverseMode.WALK;
            child.stateData.vehicleRentalNetwork = null;
            child.stateData.rentalVehicleFormFactor = null;
            child.stateData.backWalkingBike = false;
        }
    }

    /**
     * This has two effects: marks the vehicle as parked, and switches the current mode.
     * Marking the vehicle parked is important for allowing co-dominance of walking and driving states.
     */
    public void setVehicleParked(boolean vehicleParked, TraverseMode nonTransitMode) {
        cloneStateDataAsNeeded();
        child.stateData.vehicleParked = vehicleParked;
        child.stateData.currentMode = nonTransitMode;
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
        child.stateData.carPickupState = state.stateData.carPickupState;
        child.stateData.vehicleParked = state.stateData.vehicleParked;
        child.stateData.backWalkingBike = state.stateData.backWalkingBike;
    }

    public void setNonTransitOptionsFromState(State state) {
        cloneStateDataAsNeeded();
        child.stateData.currentMode = state.getNonTransitMode();
        child.stateData.vehicleParked = state.isVehicleParked();
        child.stateData.vehicleRentalState = state.stateData.vehicleRentalState;
    }

    public void setCarPickupState(CarPickupState carPickupState) {
        cloneStateDataAsNeeded();
        child.stateData.carPickupState = carPickupState;
        switch (carPickupState) {
            case WALK_TO_PICKUP:
            case WALK_FROM_DROP_OFF:
                child.stateData.currentMode = TraverseMode.WALK;
                break;
            case IN_CAR:
                child.stateData.currentMode = TraverseMode.CAR;
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
        return child.isRentingVehicle();
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

    public void setBikeRentalNetwork(String network) {
        cloneStateDataAsNeeded();
        child.stateData.vehicleRentalNetwork = network;
    }

    public State getBackState() {
        return child.getBackState();
    }
}
