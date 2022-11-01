package org.opentripplanner.routing.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.routing.algorithm.astar.NegativeWeightException;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.routing.edgetype.VehicleRentalEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.VehicleRentalPlaceVertex;
import org.opentripplanner.util.lang.ToStringBuilder;

public class State implements Cloneable {

  private final AStarRequest request;

  /* Data which is likely to change at most traversals */

  // the current time at this state, in seconds since UNIX epoch
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
   * Create an initial state, forcing vertex to the specified value. Useful for tests, etc.
   */
  public State(Vertex vertex, AStarRequest aStarRequest) {
    this(
      vertex,
      aStarRequest.startTime(),
      StateData.getInitialStateData(aStarRequest),
      aStarRequest
    );
  }

  public State(Vertex vertex, Instant startTime, StateData stateData, AStarRequest request) {
    this.request = request;
    this.weight = 0;
    this.vertex = vertex;
    this.backState = null;
    this.stateData = stateData;
    this.walkDistance = 0;
    this.time = startTime.getEpochSecond();
  }

  /**
   * Create an initial state representing the beginning of a search for the given routing request.
   * Initial "parent-less" states can only be created at the beginning of a trip. elsewhere, all
   * states must be created from a parent and associated with an edge.
   */
  public static Collection<State> getInitialStates(
    Set<Vertex> vertices,
    AStarRequest aStarRequest
  ) {
    Collection<State> states = new ArrayList<>();
    for (Vertex vertex : vertices) {
      for (StateData stateData : StateData.getInitialStateDatas(aStarRequest)) {
        states.add(new State(vertex, aStarRequest.startTime(), stateData, aStarRequest));
      }
    }
    return states;
  }

  /**
   * Create a state editor to produce a child of this state, which will be the result of traversing
   * the given edge.
   */
  public StateEditor edit(Edge e) {
    return new StateEditor(this, e);
  }

  /*
   * FIELD ACCESSOR METHODS States are immutable, so they have only get methods. The corresponding
   * set methods are in StateEditor.
   */

  public CarPickupState getCarPickupState() {
    return stateData.carPickupState;
  }

  /** Returns time in seconds since epoch */
  public long getTimeSeconds() {
    return time;
  }

  /** returns the length of the trip in seconds up to this state */
  public long getElapsedTimeSeconds() {
    return Math.abs(getTimeSeconds() - request.startTime().getEpochSecond());
  }

  public boolean isCompatibleVehicleRentalState(State state) {
    return (
      stateData.vehicleRentalState == state.stateData.vehicleRentalState &&
      stateData.mayKeepRentedVehicleAtDestination ==
      state.stateData.mayKeepRentedVehicleAtDestination
    );
  }

  public boolean isRentingVehicleFromStation() {
    return stateData.vehicleRentalState == VehicleRentalState.RENTING_FROM_STATION;
  }

  public boolean isRentingFloatingVehicle() {
    return stateData.vehicleRentalState == VehicleRentalState.RENTING_FLOATING;
  }

  public boolean isRentingVehicle() {
    return (
      stateData.vehicleRentalState == VehicleRentalState.RENTING_FROM_STATION ||
      stateData.vehicleRentalState == VehicleRentalState.RENTING_FLOATING
    );
  }

  public boolean vehicleRentalIsFinished() {
    return (
      stateData.vehicleRentalState == VehicleRentalState.HAVE_RENTED ||
      stateData.vehicleRentalState == VehicleRentalState.RENTING_FLOATING ||
      (
        getRequest().rental().allowArrivingInRentedVehicleAtDestination() &&
        stateData.mayKeepRentedVehicleAtDestination &&
        stateData.vehicleRentalState == VehicleRentalState.RENTING_FROM_STATION
      )
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
    boolean parkAndRide = request.mode().includesParking();
    boolean vehicleRentingOk;
    boolean vehicleParkAndRideOk;
    if (request.arriveBy()) {
      vehicleRentingOk = !request.mode().includesRenting() || !isRentingVehicle();
      vehicleParkAndRideOk = !parkAndRide || !isVehicleParked();
    } else {
      vehicleRentingOk =
        !request.mode().includesRenting() ||
        (vehicleRentalNotStarted() || vehicleRentalIsFinished());
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

  public double getWeightDelta() {
    return this.weight - backState.weight;
  }

  public State getBackState() {
    return this.backState;
  }

  public TraverseMode getBackMode() {
    return stateData.backMode;
  }

  public boolean isBackWalkingBike() {
    return stateData.backWalkingBike;
  }

  public Edge getBackEdge() {
    return this.backEdge;
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
   * Extend an exiting result chain by appending this result to the existing chain. The usage model
   * looks like this:
   *
   * <code>
   * TraverseResult result = null;
   * <p>
   * for( ... ) { TraverseResult individualResult = ...; result = individualResult.addToExistingResultChain(result);
   * }
   * <p>
   * return result;
   * </code>
   *
   * @param existingResultChain the tail of an existing result chain, or null if the chain has not
   *                            been started
   */
  public State addToExistingResultChain(State existingResultChain) {
    if (this.getNextResult() != null) {
      throw new IllegalStateException("this result already has a next result set");
    }
    next = existingResultChain;
    return this;
  }

  public AStarRequest getRequest() {
    return request;
  }

  public RoutingPreferences getPreferences() {
    return request.preferences();
  }

  /**
   * This method is on State rather than RouteRequest because we care whether the user is in
   * possession of a rented bike.
   *
   * @return BICYCLE if routing with an owned bicycle, or if at this state the user is holding on to
   * a rented bicycle.
   */
  public TraverseMode getNonTransitMode() {
    return stateData.currentMode;
  }

  public Instant getTime() {
    return Instant.ofEpochSecond(time);
  }

  public String getVehicleRentalNetwork() {
    return stateData.vehicleRentalNetwork;
  }

  /**
   * Reverse the path implicit in the given state, the path will be reversed but will have the same
   * duration. This is the result of combining the functions from GraphPath optimize and reverse.
   *
   * @return a state at the other end (or this end, in the case of a forward search) of a reversed
   * path
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
        var stationVertex = ((VehicleRentalPlaceVertex) orig.vertex);
        editor.dropOffRentedVehicleAtStation(
          ((VehicleRentalEdge) edge).formFactor,
          stationVertex.getStation().getNetwork(),
          false
        );
      } else if (!orig.isRentingVehicle() && orig.getBackState().isRentingVehicle()) {
        var stationVertex = ((VehicleRentalPlaceVertex) orig.vertex);
        if (orig.getBackState().isRentingVehicleFromStation()) {
          editor.beginVehicleRentingAtStation(
            ((VehicleRentalEdge) edge).formFactor,
            stationVertex.getStation().getNetwork(),
            orig.backState.mayKeepRentedVehicleAtDestination(),
            false
          );
        } else if (orig.getBackState().isRentingFloatingVehicle()) {
          editor.beginFloatingVehicleRenting(
            ((VehicleRentalEdge) edge).formFactor,
            stationVertex.getStation().getNetwork(),
            false
          );
        }
      }
      if (orig.isVehicleParked() != orig.getBackState().isVehicleParked()) {
        editor.setVehicleParked(true, orig.getBackState().getNonTransitMode());
      }

      ret = editor.makeState();

      orig = orig.getBackState();
    }

    return ret;
  }

  public boolean hasEnteredNoThruTrafficArea() {
    return stateData.enteredNoThroughTrafficArea;
  }

  public boolean mayKeepRentedVehicleAtDestination() {
    return stateData.mayKeepRentedVehicleAtDestination;
  }

  public IntersectionTraversalCalculator intersectionTraversalCalculator() {
    return request.intersectionTraversalCalculator;
  }

  public DataOverlayContext dataOverlayContext() {
    return request.dataOverlayContext;
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

  public String toString() {
    return ToStringBuilder
      .of(State.class)
      .addDateTime("time", getTime())
      .addNum("weight", weight)
      .addObj("vertex", vertex)
      .addBoolIfTrue("VEHICLE_RENT", isRentingVehicle())
      .addBoolIfTrue("VEHICLE_PARKED", isVehicleParked())
      .toString();
  }

  void checkNegativeWeight() {
    double dw = this.weight - backState.weight;
    if (dw < 0) {
      throw new NegativeWeightException(dw + " on edge " + backEdge);
    }
  }

  private int getAbsTimeDeltaSeconds() {
    return Math.abs(getTimeDeltaSeconds());
  }

  private double getWalkDistanceDelta() {
    if (backState != null) {
      return Math.abs(this.walkDistance - backState.walkDistance);
    } else {
      return 0.0;
    }
  }

  private State reversedClone() {
    AStarRequest reversedRequest = request
      .copyOfReversed(getTime())
      .withPreferences(p -> p.withRental(r -> r.withUseAvailabilityInformation(false)))
      .build();
    StateData newStateData = stateData.clone();
    newStateData.backMode = null;
    return new State(this.vertex, getTime(), newStateData, reversedRequest);
  }
}
