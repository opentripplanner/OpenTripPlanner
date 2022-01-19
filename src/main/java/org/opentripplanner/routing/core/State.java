package org.opentripplanner.routing.core;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.routing.algorithm.astar.NegativeWeightException;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.VehicleRentalEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;

public class State implements Cloneable {
    /* Data which is likely to change at most traversals */
    
    // the current time at this state, in milliseconds
    protected long time;

    // accumulated weight up to this state
    public double weight;

    // associate this state with a vertex in the graph
    protected Vertex vertex;

    // allow path reconstruction from states
    protected State backState;

    public Edge backEdge;

    // allow traverse result chaining (multiple results)
    protected State next;

    /* StateData contains data which is unlikely to change as often */
    public StateData stateData;

    // how far have we walked
    // TODO(flamholz): this is a very confusing name as it actually applies to all non-transit modes.
    // we should DEFINITELY rename this variable and the associated methods.
    public double walkDistance;

    /* CONSTRUCTORS */

    /**
     * Create an initial state representing the beginning of a search for the given routing context.
     * Initial "parent-less" states can only be created at the beginning of a trip. elsewhere, all
     * states must be created from a parent and associated with an edge.
     */
    public static Collection<State> getInitialStates(RoutingRequest request) {
        Collection<State> states = new ArrayList<>();
        for (Vertex vertex : request.rctx.fromVertices) {
            /* carPickup searches may end in two distinct states: IN_CAR and WALK_FROM_DROP_OFF/WALK_TO_PICKUP
               for forward/reverse searches to be symmetric both initial states need to be created. */
            if (request.carPickup) {
                states.add(
                    new State(
                    vertex,
                    request.rctx.originBackEdge,
                    request.getSecondsSinceEpoch(),
                    request,
                    true,
                    false,
                    false
                ));
            }

            /* vehicle rental searches may end in three states (see isFinal()): BEFORE_RENTING/RENTING_FLOATING/HAVE_RENTED
               for forward/reverse searches to be symmetric an additional RENTING_FLOATING state needs to be created. */
            if (request.vehicleRental && request.arriveBy) {
                states.add(
                    new State(
                        vertex,
                        request.rctx.originBackEdge,
                        request.getSecondsSinceEpoch(),
                        request,
                        false,
                        true,
                        false
                    ));

                if (request.allowKeepingRentedVehicleAtDestination) {
                    states.add(
                            new State(
                                    vertex,
                                    request.rctx.originBackEdge,
                                    request.getSecondsSinceEpoch(),
                                    request,
                                    false,
                                    true,
                                    true
                    ));
                }
            }

            states.add(new State(
                vertex,
                request.rctx.originBackEdge,
                request.getSecondsSinceEpoch(),
                request
            ));
        }
        return states;
    }

    public State(RoutingRequest opt) {
        this(
                opt.rctx.fromVertices == null ? null : opt.rctx.fromVertices.iterator().next(),
                opt.rctx.originBackEdge,
                opt.getSecondsSinceEpoch(),
                opt
        );
    }

    /**
     * Create an initial state, forcing vertex to the specified value. Useful for reusing a 
     * RoutingContext in TransitIndex, tests, etc.
     */
    public State(Vertex vertex, RoutingRequest opt) {
        // Since you explicitly specify, the vertex, we don't set the backEdge.
        this(vertex, opt.getSecondsSinceEpoch(), opt);
    }

    /**
     * Create an initial state, forcing vertex and time to the specified values. Useful for reusing 
     * a RoutingContext in TransitIndex, tests, etc.
     */
    public State(Vertex vertex, long timeSeconds, RoutingRequest options) {
        // Since you explicitly specify, the vertex, we don't set the backEdge.
        this(vertex, null, timeSeconds, options);
    }
    
    /**
     * Create an initial state, forcing vertex, back edge and time to the specified values. Useful for reusing 
     * a RoutingContext in TransitIndex, tests, etc.
     */
    public State(Vertex vertex, Edge backEdge, long timeSeconds, RoutingRequest options) {
        this(vertex, backEdge, timeSeconds, timeSeconds, options, false, false, false);
    }

    /**
     * Create an initial state, forcing vertex, back edge and time to the specified values. Useful for reusing
     * a RoutingContext in TransitIndex, tests, etc.
     */
    public State(
        Vertex vertex,
        Edge backEdge,
        long timeSeconds,
        RoutingRequest options,
        boolean carPickupStateInCar,
        boolean bikeRentalFloatingState,
        boolean keptRentedVehicleAtDestination) {
        this(vertex, backEdge, timeSeconds, timeSeconds, options, carPickupStateInCar, bikeRentalFloatingState, keptRentedVehicleAtDestination);
    }

    /**
     * Create an initial state, forcing vertex, back edge, time and start time to the specified values. Useful for starting
     * a multiple initial state search, for example when propagating profile results to the street network in RoundBasedProfileRouter.
     */
    public State(
        Vertex vertex,
        Edge backEdge,
        long timeSeconds,
        long startTime,
        RoutingRequest options,
        boolean carPickupStateInCar,
        boolean vehicleRentalFloatingState,
        boolean keptRentedVehicleAtDestination
    ) {
        this.weight = 0;
        this.vertex = vertex;
        this.backEdge = backEdge;
        this.backState = null;
        this.stateData = new StateData(options);
        // note that here we are breaking the circular reference between rctx and options
        // this should be harmless since reversed clones are only used when routing has finished
        this.stateData.opt = options;
        this.stateData.startTime = startTime;
        if (options.vehicleRental) {
            if (options.arriveBy) {
                if (keptRentedVehicleAtDestination) {
                    this.stateData.vehicleRentalState = VehicleRentalState.RENTING_FROM_STATION;
                    this.stateData.currentMode = TraverseMode.BICYCLE;
                    this.stateData.mayKeepRentedVehicleAtDestination = true;
                } else if (vehicleRentalFloatingState) {
                    this.stateData.vehicleRentalState = VehicleRentalState.RENTING_FLOATING;
                    this.stateData.currentMode = TraverseMode.BICYCLE;
                } else {
                    this.stateData.vehicleRentalState = VehicleRentalState.HAVE_RENTED;
                    this.stateData.currentMode = TraverseMode.WALK;
                }
            }
            else {
                this.stateData.vehicleRentalState = VehicleRentalState.BEFORE_RENTING;
            }
        }
        if (options.carPickup) {
            /* For carPickup two initial states are created in getStates(request):
                 1. WALK / WALK_FROM_DROP_OFF or WALK_TO_PICKUP for cases with an initial walk
                 2. CAR / IN_CAR where pickup happens directly at the bus stop */
            if (carPickupStateInCar) {
                this.stateData.carPickupState = CarPickupState.IN_CAR;
                this.stateData.currentMode = TraverseMode.CAR;
            } else {
                this.stateData.carPickupState = options.arriveBy ? CarPickupState.WALK_FROM_DROP_OFF : CarPickupState.WALK_TO_PICKUP;
                this.stateData.currentMode = TraverseMode.WALK;
            }
        }
        /* If the itinerary is to begin with a car that is left for transit, the initial state of arriveBy searches is
           with the car already "parked" and in WALK mode. Otherwise, we are in CAR mode and "unparked". */
        if (options.parkAndRide) {
            this.stateData.vehicleParked = options.arriveBy;
            this.stateData.currentMode = this.stateData.vehicleParked ?
                TraverseMode.WALK
                : options.streetSubRequestModes.getBicycle() ?
                    TraverseMode.BICYCLE : TraverseMode.CAR;
        }
        this.walkDistance = 0;
        this.time = timeSeconds * 1000;
    }

    /**
     * Create a state editor to produce a child of this state, which will be the result of
     * traversing the given edge.
     * 
     * @param e
     * @return
     */
    public StateEditor edit(Edge e) {
        return new StateEditor(this, e);
    }

    protected State clone() {
        State ret;
        try {
            ret = (State) super.clone();
        } catch (CloneNotSupportedException e1) {
            throw new IllegalStateException("This is not happening");
        }
        return ret;
    }

    /*
     * FIELD ACCESSOR METHODS States are immutable, so they have only get methods. The corresponding
     * set methods are in StateEditor.
     */

    public String toString() {
        return "<State " + new Date(getTimeInMillis()) + " [" + weight + "] "
                + (isRentingVehicle() ? "VEHICLE_RENT " : "") + (isVehicleParked() ? "VEHICLE_PARKED " : "")
                + vertex + ">";
    }
    
    public String toStringVerbose() {
        return "<State " + new Date(getTimeInMillis()) + 
                " w=" + this.getWeight() + 
                " t=" + this.getElapsedTimeSeconds() + 
                " d=" + this.getWalkDistance() +
                " r=" + this.isRentingVehicle() +
                " pr=" + this.isVehicleParked() + ">";
    }

    public CarPickupState getCarPickupState() {
        return stateData.carPickupState;
    }
    
    /** Returns time in seconds since epoch */
    public long getTimeSeconds() {
        return time / 1000;
    }

    /** returns the length of the trip in seconds up to this state */
    public long getElapsedTimeSeconds() {
        return Math.abs(getTimeSeconds() - stateData.startTime);
    }

    public boolean isCompatibleVehicleRentalState(State state) {
        return stateData.vehicleRentalState == state.stateData.vehicleRentalState &&
                stateData.mayKeepRentedVehicleAtDestination
                        == state.stateData.mayKeepRentedVehicleAtDestination;
    }

    public boolean isRentingVehicleFromStation() {
        return stateData.vehicleRentalState == VehicleRentalState.RENTING_FROM_STATION;
    }

    public boolean isRentingFloatingVehicle() {
        return stateData.vehicleRentalState == VehicleRentalState.RENTING_FLOATING;
    }

    public boolean isRentingVehicle() {
        return stateData.vehicleRentalState == VehicleRentalState.RENTING_FROM_STATION
            || stateData.vehicleRentalState == VehicleRentalState.RENTING_FLOATING;
    }

    public boolean vehicleRentalIsFinished() {
        return stateData.vehicleRentalState == VehicleRentalState.HAVE_RENTED
                || stateData.vehicleRentalState == VehicleRentalState.RENTING_FLOATING
                || (
                getOptions().allowKeepingRentedVehicleAtDestination
                        && stateData.mayKeepRentedVehicleAtDestination
                        && stateData.vehicleRentalState == VehicleRentalState.RENTING_FROM_STATION
        );
    }

    public boolean vehicleRentalNotStarted() {
        return stateData.vehicleRentalState == VehicleRentalState.BEFORE_RENTING;
    }

    public VehicleRentalState getVehicleRentalState() {
        return stateData.vehicleRentalState;
    }

    public boolean isVehicleParked() {
        return stateData.vehicleParked;
    }

    /**
     * @return True if the state at vertex can be the end of path.
     */
    public boolean isFinal() {
        // When drive-to-transit is enabled, we need to check whether the car has been parked (or whether it has been picked up in reverse).
        boolean parkAndRide = stateData.opt.parkAndRide;
        boolean vehicleRentingOk;
        boolean vehicleParkAndRideOk;
        if (stateData.opt.arriveBy) {
            vehicleRentingOk = !stateData.opt.vehicleRental || !isRentingVehicle();
            vehicleParkAndRideOk = !parkAndRide || !isVehicleParked();
        } else {
            vehicleRentingOk = !stateData.opt.vehicleRental || (vehicleRentalNotStarted() || vehicleRentalIsFinished());
            vehicleParkAndRideOk = !parkAndRide || isVehicleParked();
        }
        return vehicleRentingOk && vehicleParkAndRideOk;
    }

    public double getWalkDistance() {
        return walkDistance;
    }

    public Vertex getVertex() {
        return this.vertex;
    }

    public double getWeight() {
        return this.weight;
    }

    public int getTimeDeltaSeconds() {
        return backState != null ? (int) (getTimeSeconds() - backState.getTimeSeconds()) : 0;
    }

    private int getAbsTimeDeltaSeconds() {
        return Math.abs(getTimeDeltaSeconds());
    }

    private double getWalkDistanceDelta () {
        if (backState != null) {
            return Math.abs(this.walkDistance - backState.walkDistance);
        }
        else {
            return 0.0;
        }
    }

    public double getWeightDelta() {
        return this.weight - backState.weight;
    }

    void checkNegativeWeight() {
        double dw = this.weight - backState.weight;
        if (dw < 0) {
            throw new NegativeWeightException(String.valueOf(dw) + " on edge " + backEdge);
        }
    }

    public State getBackState() {
        return this.backState;
    }
    
    public TraverseMode getBackMode () {
        return stateData.backMode;
    }
    
    public boolean isBackWalkingBike () {
        return stateData.backWalkingBike;
    }

    public Edge getBackEdge() {
        return this.backEdge;
    }

    public long getStartTimeSeconds() {
        return stateData.startTime;
    }

    /**
     * Optional next result that allows {@link Edge} to return multiple results.
     * 
     * @return the next additional result from an edge traversal, or null if no more results
     */
    public State getNextResult() {
        return next;
    }

    /**
     * Extend an exiting result chain by appending this result to the existing chain. The usage
     * model looks like this:
     * 
     * <code>
     * TraverseResult result = null;
     * 
     * for( ... ) {
     *   TraverseResult individualResult = ...;
     *   result = individualResult.addToExistingResultChain(result);
     * }
     * 
     * return result;
     * </code>
     * 
     * @param existingResultChain the tail of an existing result chain, or null if the chain has not
     *        been started
     * @return
     */
    public State addToExistingResultChain(State existingResultChain) {
        if (this.getNextResult() != null) {
            throw new IllegalStateException("this result already has a next result set");
        }
        next = existingResultChain;
        return this;
    }

    public RoutingContext getContext() {
        return stateData.opt.rctx;
    }

    public RoutingRequest getOptions () {
        return stateData.opt;
    }
    
    /**
     * This method is on State rather than RoutingRequest because we care whether the user is in
     * possession of a rented bike.
     * 
     * @return BICYCLE if routing with an owned bicycle, or if at this state the user is holding on
     *         to a rented bicycle.
     */
    public TraverseMode getNonTransitMode() {
        return stateData.currentMode;
    }
    // TODO: There is no documentation about what this means. No one knows precisely.
    // Needs to be replaced with clearly defined fields.

    private State reversedClone() {
        // We no longer compensate for schedule slack (minTransferTime) here.
        // It is distributed symmetrically over all preboard and prealight edges.
        State newState = new State(this.vertex, getTimeSeconds(), stateData.opt.reversedClone());
        // TODO Check if those two lines are needed:
        newState.stateData.vehicleRentalState = stateData.vehicleRentalState;
        newState.stateData.vehicleParked = stateData.vehicleParked;
        newState.stateData.carPickupState = stateData.carPickupState;
        newState.stateData.timeRestrictions = new ArrayList<>(getTimeRestrictions());
        newState.stateData.timeRestrictionSources = new HashSet<>(getTimeRestrictionSources());
        return newState;
    }

    public void dumpPath() {
        System.out.printf("---- FOLLOWING CHAIN OF STATES ----\n");
        State s = this;
        while (s != null) {
            System.out.printf("%s via %s by %s\n", s, s.backEdge, s.getBackMode());
            s = s.backState;
        }
        System.out.printf("---- END CHAIN OF STATES ----\n");
    }

    public long getTimeInMillis() {
        return time;
    }

    public ZonedDateTime getTimeAsZonedDateTime() {
        return Instant.ofEpochMilli(getTimeInMillis())
                .atZone(getOptions().rctx.graph.getTimeZone().toZoneId());
    }

    public LocalDateTime getTimeAsLocalDateTime() {
        return getTimeAsZonedDateTime()
                .toLocalDateTime();
    }

    public boolean multipleOptionsBefore() {
        boolean foundAlternatePaths = false;
        TraverseMode requestedMode = getNonTransitMode();
        for (Edge out : backState.vertex.getOutgoing()) {
            if (out == backEdge) {
                continue;
            }
            if (!(out instanceof StreetEdge)) {
                continue;
            }
            State outState = out.traverse(backState);
            if (outState == null) {
                continue;
            }
            if (!outState.getBackMode().equals(requestedMode)) {
                //walking a bike, so, not really an exit
                continue;
            }
            // this section handles the case of an option which is only an option if you walk your
            // bike. It is complicated because you will not need to walk your bike until one
            // edge after the current edge.

            //now, from here, try a continuing path.
            Vertex tov = outState.getVertex();
            boolean found = false;
            for (Edge out2 : tov.getOutgoing()) {
                State outState2 = out2.traverse(outState);
                if (outState2 != null && !Objects.equals(outState2.getBackMode(), requestedMode)) {
                    // walking a bike, so, not really an exit
                    continue;
                }
                found = true;
                break;
            }
            if (!found) {
                continue;
            }

            // there were paths we didn't take.
            foundAlternatePaths = true;
            break;
        }
        return foundAlternatePaths;
    }

    public String getVehicleRentalNetwork() {
        return stateData.vehicleRentalNetwork;
    }

    /**
     * Reverse the path implicit in the given state, re-traversing all edges in the opposite
     * direction so as to remove any unnecessary waiting in the resulting itinerary. This produces a
     * path that passes through all the same edges, but which may have a shorter overall duration
     * due to different weights on time-dependent (e.g. transit boarding) edges. If the optimize 
     * parameter is false, the path will be reversed but will have the same duration. This is the 
     * result of combining the functions from GraphPath optimize and reverse.
     *
     * @return a state at the other end (or this end, in the case of a forward search)
     * of a reversed, optimized path
     */
    public State reverse() {
        State orig = this;
        State ret = orig.reversedClone();

        Edge edge;

        while (orig.getBackState() != null) {
            edge = orig.getBackEdge();

            // Not reverse-optimizing, so we don't re-traverse the edges backward.
            // Instead we just replicate all the states, and replicate the deltas between the state's incremental fields.
            // TODO determine whether this is really necessary, and whether there's a more maintainable way to do this.
            StateEditor editor = ret.edit(edge);
            // note the distinction between setFromState and setBackState
            editor.setFromState(orig);

            editor.incrementTimeInSeconds(orig.getAbsTimeDeltaSeconds());
            editor.incrementWeight(orig.getWeightDelta());
            editor.incrementWalkDistance(orig.getWalkDistanceDelta());

            // propagate the modes through to the reversed edge
            editor.setBackMode(orig.getBackMode());

            if (orig.isRentingVehicle() && !orig.getBackState().isRentingVehicle()) {
                var stationVertex = ((VehicleRentalStationVertex) orig.vertex);
                editor.dropOffRentedVehicleAtStation(
                        ((VehicleRentalEdge) edge).formFactor,
                        stationVertex.getStation().getNetwork(),
                        false
                );
            }
            else if (!orig.isRentingVehicle() && orig.getBackState().isRentingVehicle()) {
                var stationVertex = ((VehicleRentalStationVertex) orig.vertex);
                if (orig.getBackState().isRentingVehicleFromStation()) {
                    editor.beginVehicleRentingAtStation(
                            ((VehicleRentalEdge) edge).formFactor,
                            stationVertex.getStation().getNetwork(),
                            orig.backState.mayKeepRentedVehicleAtDestination(),
                            false
                    );
                }
                else if (orig.getBackState().isRentingFloatingVehicle()) {
                    editor.beginFloatingVehicleRenting(
                            ((VehicleRentalEdge) edge).formFactor,
                            stationVertex.getStation().getNetwork(),
                            false
                    );
                }
            }
            if (orig.isVehicleParked() != orig.getBackState().isVehicleParked()) {
                editor.setVehicleParked(true, orig.getNonTransitMode());
            }

            ret = editor.makeState();

            orig = orig.getBackState();
        }

        return ret;
    }

    /**
     * Reverse-optimize a path after it is complete, by default
     */
    public State optimize() {
        return reverse();
    }

    public boolean hasEnteredNoThruTrafficArea() {
        return stateData.enteredNoThroughTrafficArea;
    }

    public boolean mayKeepRentedVehicleAtDestination() {
        return stateData.mayKeepRentedVehicleAtDestination;
    }

    public List<TimeRestrictionWithOffset> getTimeRestrictions() {
        if (stateData.timeRestrictions == null) {
            return List.of();
        }
        return stateData.timeRestrictions;
    }

    public Set<Object> getTimeRestrictionSources() {
        if (stateData.timeRestrictions == null) {
            return Set.of();
        }
        return stateData.timeRestrictionSources;
    }
}
