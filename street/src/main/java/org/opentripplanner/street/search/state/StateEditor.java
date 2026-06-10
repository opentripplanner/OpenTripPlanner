package org.opentripplanner.street.search.state;

import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType.PropulsionType;
import org.opentripplanner.street.mapping.StreetModeToRentalTraverseModeMapper;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
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
  private final StreetSearchRequest request;
  private final State backState;
  private final Edge backEdge;
  private final Vertex vertex;
  private StateData stateData;
  private double weight;
  private long time_ms;
  private double traversalDistance_m;

  private boolean traversingBackward;

  /* CONSTRUCTORS */

  /**
   * The very first state in the chain before any iteration has started.
   */
  public StateEditor(Vertex v, StreetSearchRequest request) {
    this.request = request;
    this.stateData = new State(v, request).stateData;
    this.backState = null;
    this.backEdge = null;
    this.vertex = v;
    this.time_ms = request.startTime().toEpochMilli();
    this.weight = 0;
    this.traversalDistance_m = 0;
  }

  public StateEditor(State parent, Edge e) {
    this.request = parent.getRequest();
    this.stateData = parent.stateData;
    this.backState = parent;
    this.backEdge = e;
    this.time_ms = parent.time_ms;
    this.weight = parent.weight;
    this.traversalDistance_m = parent.traversalDistance_m;

    final Vertex parentVertex = parent.vertex;

    final Vertex fromVertex = e.getFromVertex();
    final Vertex toVertex = e.getToVertex();

    // Note that we use equals(), not ==, here to allow for dynamically created vertices
    if (parentVertex.equals(fromVertex)) {
      // from and to vertices are the same on eg. vehicle rental and parking vertices, thus, we
      // can't know the direction of travel from the above check. The expression below is simplified
      // fromVertex.equals(toVertex) ? parent.getOptions().arriveBy : false;
      traversingBackward = fromVertex.equals(toVertex) && parent.getRequest().arriveBy();
      this.vertex = toVertex;
    } else if (parentVertex.equals(toVertex)) {
      traversingBackward = true;
      this.vertex = fromVertex;
    } else {
      throw new IllegalStateException(
        "Edge is not connected to parent state: %s, from=%s, to=%s, parent=%s".formatted(
          e,
          fromVertex,
          toVertex,
          parentVertex
        )
      );
    }

    if (traversingBackward != parent.getRequest().arriveBy()) {
      throw new IllegalStateException(
        "Actual traversal direction does not match traversal direction in %s".formatted(
          parent.getRequest()
        )
      );
    }
  }

  /* PUBLIC METHODS */

  /**
   *
   * Why can a state editor only be used once? If you modify some component of state with and
   * editor, use the editor to create a new state, and then make more modifications, these
   * modifications will be applied to the previously created state. Reusing the state editor to make
   * several states would modify an existing state somewhere earlier in the search, messing up the
   * shortest path tree.
   */
  @Nullable
  public State makeState() {
    if (backState != null) {
      // check that time changes are coherent with edge traversal
      // direction
      double timeDelta = time_ms - backState.getTimeMilliseconds();
      if (traversingBackward ? (timeDelta > 0) : (timeDelta < 0)) {
        LOG.trace("Time was incremented the wrong direction during state editing. {}", backEdge);
        return null;
      }
    }
    return new State(
      request,
      weight,
      vertex,
      backState,
      backEdge,
      stateData,
      traversalDistance_m,
      time_ms
    );
  }

  /**
   * Calls {@link StateEditor#makeState()} and wraps the result in an array of {@link State}.
   * If the state is null, then a zero-length array is returned.
   */
  public State[] makeStateArray() {
    return State.ofNullable(makeState());
  }

  public String toString() {
    return "StateEditor{" + backState + "}";
  }

  /* PUBLIC METHODS TO MODIFY A STATE BEFORE IT IS USED */

  /* Incrementors */

  public void incrementWeight(double weight) {
    if (Double.isInfinite(weight) || Double.isNaN(weight)) {
      throw new IllegalArgumentException(
        "A state's weight is being incremented by " + weight + " while traversing edge " + backEdge
      );
    }
    if (weight < 0) {
      throw new IllegalArgumentException(
        "A state's weight is being incremented by a negative amount while traversing edge " +
          backEdge
      );
    }
    this.weight += weight;
  }

  /**
   * Advance or rewind the time of the new state by the given non-negative amount. Direction of time
   * is inferred from the direction of traversal. This is the only element of state that runs
   * backward when traversing backward.
   */
  public void incrementTimeInMilliseconds(long milliseconds) {
    if (milliseconds < 0) {
      throw new IllegalArgumentException(
        "A state's time is being incremented by a negative amount while traversing edge " + backEdge
      );
    }
    this.time_ms += (traversingBackward ? -milliseconds : milliseconds);
  }

  public void incrementTimeInSeconds(long seconds) {
    incrementTimeInMilliseconds(1000L * seconds);
  }

  /**
   * Increment the distance traversed through the graph in meters.
   */
  public void incrementTraversalDistanceMeters(double length) {
    if (length < 0) {
      throw new IllegalArgumentException("Traversal distance cannot be negative");
    }
    this.traversalDistance_m += length;
  }

  /* Basic Setters */

  public void resetEnteredNoThroughTrafficArea() {
    if (!stateData.enteredNoThroughTrafficArea) {
      return;
    }

    cloneStateDataAsNeeded();
    stateData.enteredNoThroughTrafficArea = false;
  }

  public void setEnteredNoThroughTrafficArea() {
    if (stateData.enteredNoThroughTrafficArea) {
      return;
    }

    cloneStateDataAsNeeded();
    stateData.enteredNoThroughTrafficArea = true;
  }

  public void leaveNoRentalDropOffArea() {
    if (!stateData.insideNoRentalDropOffArea) {
      return;
    }

    cloneStateDataAsNeeded();
    stateData.insideNoRentalDropOffArea = false;
  }

  public void enterNoRentalDropOffArea() {
    if (stateData.insideNoRentalDropOffArea) {
      return;
    }

    cloneStateDataAsNeeded();
    stateData.insideNoRentalDropOffArea = true;
  }

  public void setBackMode(TraverseMode mode) {
    if (mode == stateData.backMode) {
      return;
    }

    cloneStateDataAsNeeded();
    stateData.backMode = mode;
  }

  public void setBackWalkingBike(boolean walkingBike) {
    if (walkingBike == stateData.backWalkingBike) {
      return;
    }

    cloneStateDataAsNeeded();
    stateData.backWalkingBike = walkingBike;
  }

  public void beginFloatingVehicleRenting(
    RentalFormFactor formFactor,
    PropulsionType propulsionType,
    String network,
    boolean reverse
  ) {
    cloneStateDataAsNeeded();
    if (reverse) {
      stateData.vehicleRentalState = VehicleRentalState.BEFORE_RENTING;
      stateData.currentMode = TraverseMode.WALK;
      stateData.vehicleRentalNetwork = null;
      stateData.rentalVehicleFormFactor = null;
      stateData.rentalVehiclePropulsionType = null;
      stateData.insideNoRentalDropOffArea = false;
    } else {
      stateData.vehicleRentalState = VehicleRentalState.RENTING_FLOATING;
      stateData.currentMode = formFactor.traverseMode;
      stateData.vehicleRentalNetwork = network;
      stateData.rentalVehicleFormFactor = formFactor;
      stateData.rentalVehiclePropulsionType = propulsionType;
    }
  }

  public void beginVehicleRentingAtStation(
    RentalFormFactor formFactor,
    PropulsionType propulsionType,
    String network,
    boolean mayKeep,
    boolean reverse
  ) {
    cloneStateDataAsNeeded();
    if (reverse) {
      stateData.mayKeepRentedVehicleAtDestination = mayKeep;
      stateData.vehicleRentalState = VehicleRentalState.BEFORE_RENTING;
      stateData.currentMode = TraverseMode.WALK;
      stateData.vehicleRentalNetwork = null;
      stateData.rentalVehicleFormFactor = null;
      stateData.rentalVehiclePropulsionType = null;
      stateData.backWalkingBike = false;
    } else {
      stateData.mayKeepRentedVehicleAtDestination = mayKeep;
      stateData.vehicleRentalState = VehicleRentalState.RENTING_FROM_STATION;
      stateData.currentMode = formFactor.traverseMode;
      stateData.vehicleRentalNetwork = network;
      stateData.rentalVehicleFormFactor = formFactor;
      stateData.rentalVehiclePropulsionType = propulsionType;
    }
  }

  public void dropOffRentedVehicleAtStation(
    RentalFormFactor formFactor,
    PropulsionType propulsionType,
    String network,
    boolean reverse
  ) {
    cloneStateDataAsNeeded();
    if (reverse) {
      stateData.mayKeepRentedVehicleAtDestination = false;
      stateData.vehicleRentalState = VehicleRentalState.RENTING_FROM_STATION;
      stateData.currentMode = formFactor.traverseMode;
      stateData.vehicleRentalNetwork = network;
      stateData.rentalVehicleFormFactor = formFactor;
      stateData.rentalVehiclePropulsionType = propulsionType;
    } else {
      stateData.mayKeepRentedVehicleAtDestination = false;
      stateData.vehicleRentalState = VehicleRentalState.HAVE_RENTED;
      stateData.currentMode = TraverseMode.WALK;
      stateData.vehicleRentalNetwork = null;
      stateData.rentalVehicleFormFactor = null;
      stateData.rentalVehiclePropulsionType = null;
      stateData.backWalkingBike = false;
    }
  }

  public void dropFloatingVehicle(
    RentalFormFactor formFactor,
    PropulsionType propulsionType,
    String network,
    boolean reverse
  ) {
    cloneStateDataAsNeeded();
    if (reverse) {
      stateData.mayKeepRentedVehicleAtDestination = false;
      stateData.vehicleRentalState = VehicleRentalState.RENTING_FLOATING;
      stateData.currentMode = formFactor != null
        ? formFactor.traverseMode
        : StreetModeToRentalTraverseModeMapper.map(request.mode());
      stateData.vehicleRentalNetwork = network;
      stateData.rentalVehicleFormFactor = formFactor;
      stateData.rentalVehiclePropulsionType = propulsionType;
    } else {
      stateData.mayKeepRentedVehicleAtDestination = false;
      stateData.vehicleRentalState = VehicleRentalState.HAVE_RENTED;
      stateData.currentMode = TraverseMode.WALK;
      stateData.vehicleRentalNetwork = null;
      stateData.rentalVehicleFormFactor = null;
      stateData.rentalVehiclePropulsionType = null;
      stateData.backWalkingBike = false;
    }
  }

  /**
   * This has two effects: marks the vehicle as parked, and switches the current mode. Marking the
   * vehicle parked is important for allowing co-dominance of walking and driving states.
   */
  public void setVehicleParked(boolean vehicleParked, TraverseMode nonTransitMode) {
    // reset through traffic limitations when street mode changes to allow park & ride
    resetEnteredNoThroughTrafficArea();

    cloneStateDataAsNeeded();
    stateData.vehicleParked = vehicleParked;
    stateData.currentMode = nonTransitMode;
  }

  /**
   * Set non-incremental state values from an existing state. Incremental values are not currently
   * set.
   */
  public void setFromState(State state) {
    cloneStateDataAsNeeded();
    stateData.currentMode = state.stateData.currentMode;
    stateData.carPickupState = state.stateData.carPickupState;
    stateData.vehicleParked = state.stateData.vehicleParked;
    stateData.backWalkingBike = state.stateData.backWalkingBike;
  }

  public void setCarPickupState(CarPickupState carPickupState) {
    cloneStateDataAsNeeded();
    stateData.carPickupState = carPickupState;
    switch (carPickupState) {
      case WALK_TO_PICKUP, WALK_FROM_DROP_OFF -> stateData.currentMode = TraverseMode.WALK;
      case IN_CAR -> stateData.currentMode = TraverseMode.CAR;
    }
  }

  public void setTimeSeconds(long seconds) {
    this.time_ms = 1000 * seconds;
  }

  /* PUBLIC GETTER METHODS */

  public State getBackState() {
    return backState;
  }

  public void resetStartedInNoDropOffZone() {
    cloneStateDataAsNeeded();
    stateData.noRentalDropOffZonesAtStartOfReverseSearch = Set.of();
  }

  /* PRIVATE METHODS */

  /**
   * To be called before modifying anything in the child's StateData. Makes sure that changes are
   * applied to a copy of StateData rather than the same one that is still referenced in existing,
   * older states.
   */
  private void cloneStateDataAsNeeded() {
    if (backState != null && stateData == backState.stateData) {
      this.stateData = backState.stateData.clone();
    }
  }
}
