package org.opentripplanner.street.search.state;

import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.routing.algorithm.mapping.StreetModeToRentalTraverseModeMapper;
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

  protected State child;

  private boolean spawned = false;

  private boolean defectiveTraversal = false;

  private boolean traversingBackward;

  /* CONSTRUCTORS */

  public StateEditor(Vertex v, StreetSearchRequest request) {
    child = new State(v, request);
  }

  public StateEditor(State parent, Edge e) {
    child = parent.clone();
    child.backState = parent;
    child.backEdge = e;

    final Vertex parentVertex = parent.vertex;

    if (e == null) {
      child.backState = null;
      child.vertex = parentVertex;
      child.stateData = child.stateData.clone();
      return;
    }

    final Vertex fromVertex = e.getFromVertex();
    final Vertex toVertex = e.getToVertex();

    if (fromVertex == null || toVertex == null) {
      child.vertex = parentVertex;
      child.stateData = child.stateData.clone();
      LOG.error("From or to vertex is null for {}", e);
      defectiveTraversal = true;
      return;
    }

    // Note that we use equals(), not ==, here to allow for dynamically created vertices
    if (parentVertex.equals(fromVertex)) {
      // from and to vertices are the same on eg. vehicle rental and parking vertices, thus, we
      // can't know the direction of travel from the above check. The expression below is simplified
      // fromVertex.equals(toVertex) ? parent.getOptions().arriveBy : false;
      traversingBackward = fromVertex.equals(toVertex) && parent.getRequest().arriveBy();
      child.vertex = toVertex;
    } else if (parentVertex.equals(toVertex)) {
      traversingBackward = true;
      child.vertex = fromVertex;
    } else {
      // Parent state is not at either end of edge.
      LOG.warn("Edge is not connected to parent state: {}", e);
      LOG.warn("   from   vertex: {}", fromVertex);
      LOG.warn("   to     vertex: {}", toVertex);
      LOG.warn("   parent vertex: {}", parentVertex);
      defectiveTraversal = true;
    }

    if (traversingBackward != parent.getRequest().arriveBy()) {
      LOG.error(
        "Actual traversal direction does not match traversal direction in TraverseOptions."
      );
      defectiveTraversal = true;
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
    // check that this editor has not been used already
    if (spawned) throw new IllegalStateException("A StateEditor can only be used once.");

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
      if (
        traversingBackward
          ? (child.getTimeDeltaMilliseconds() > 0)
          : (child.getTimeDeltaMilliseconds() < 0)
      ) {
        LOG.trace(
          "Time was incremented the wrong direction during state editing. {}",
          child.backEdge
        );
        return null;
      }
    }
    spawned = true;
    return child;
  }

  /**
   * Calls {@link StateEditor#makeState()} and wraps the result in an array of {@link State}.
   * If the state is null, then a zero-length array is returned.
   */
  public State[] makeStateArray() {
    return State.ofNullable(makeState());
  }

  public String toString() {
    return "StateEditor{" + child + "}";
  }

  /* PUBLIC METHODS TO MODIFY A STATE BEFORE IT IS USED */

  /* Incrementors */

  public void incrementWeight(double weight) {
    if (Double.isInfinite(weight) || Double.isNaN(weight)) {
      LOG.warn(
        "A state's weight is being incremented by " +
        weight +
        " while traversing edge " +
        child.backEdge
      );
      defectiveTraversal = true;
      return;
    }
    if (weight < 0) {
      LOG.warn(
        "A state's weight is being incremented by a negative amount while traversing edge " +
        child.backEdge
      );
      defectiveTraversal = true;
      return;
    }
    child.weight += weight;
  }

  /**
   * Advance or rewind the time of the new state by the given non-negative amount. Direction of time
   * is inferred from the direction of traversal. This is the only element of state that runs
   * backward when traversing backward.
   */
  public void incrementTimeInMilliseconds(long milliseconds) {
    if (milliseconds < 0) {
      LOG.warn(
        "A state's time is being incremented by a negative amount while traversing edge " +
        child.getBackEdge()
      );
      defectiveTraversal = true;
      return;
    }
    child.time_ms += (traversingBackward ? -milliseconds : milliseconds);
  }

  public void incrementTimeInSeconds(long seconds) {
    incrementTimeInMilliseconds(1000L * seconds);
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

  public void leaveNoRentalDropOffArea() {
    if (!child.stateData.insideNoRentalDropOffArea) {
      return;
    }

    cloneStateDataAsNeeded();
    child.stateData.insideNoRentalDropOffArea = false;
  }

  public void enterNoRentalDropOffArea() {
    if (child.stateData.insideNoRentalDropOffArea) {
      return;
    }

    cloneStateDataAsNeeded();
    child.stateData.insideNoRentalDropOffArea = true;
  }

  public void setBackMode(TraverseMode mode) {
    if (mode == child.stateData.backMode) return;

    cloneStateDataAsNeeded();
    child.stateData.backMode = mode;
  }

  public void setBackWalkingBike(boolean walkingBike) {
    if (walkingBike == child.stateData.backWalkingBike) return;

    cloneStateDataAsNeeded();
    child.stateData.backWalkingBike = walkingBike;
  }

  public void beginFloatingVehicleRenting(
    RentalFormFactor formFactor,
    String network,
    boolean reverse
  ) {
    cloneStateDataAsNeeded();
    if (reverse) {
      child.stateData.vehicleRentalState = VehicleRentalState.BEFORE_RENTING;
      child.stateData.currentMode = TraverseMode.WALK;
      child.stateData.vehicleRentalNetwork = null;
      child.stateData.rentalVehicleFormFactor = null;
      child.stateData.insideNoRentalDropOffArea = false;
    } else {
      child.stateData.vehicleRentalState = VehicleRentalState.RENTING_FLOATING;
      child.stateData.currentMode = formFactor.traverseMode;
      child.stateData.vehicleRentalNetwork = network;
      child.stateData.rentalVehicleFormFactor = formFactor;
    }
  }

  public void beginVehicleRentingAtStation(
    RentalFormFactor formFactor,
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
    RentalFormFactor formFactor,
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

  public void dropFloatingVehicle(RentalFormFactor formFactor, String network, boolean reverse) {
    cloneStateDataAsNeeded();
    if (reverse) {
      child.stateData.mayKeepRentedVehicleAtDestination = false;
      child.stateData.vehicleRentalState = VehicleRentalState.RENTING_FLOATING;
      child.stateData.currentMode = formFactor != null
        ? formFactor.traverseMode
        : StreetModeToRentalTraverseModeMapper.map(child.getRequest().mode());
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
   * This has two effects: marks the vehicle as parked, and switches the current mode. Marking the
   * vehicle parked is important for allowing co-dominance of walking and driving states.
   */
  public void setVehicleParked(boolean vehicleParked, TraverseMode nonTransitMode) {
    // reset through traffic limitations when street mode changes to allow park & ride
    resetEnteredNoThroughTrafficArea();

    cloneStateDataAsNeeded();
    child.stateData.vehicleParked = vehicleParked;
    child.stateData.currentMode = nonTransitMode;
  }

  /**
   * Set non-incremental state values from an existing state. Incremental values are not currently
   * set.
   */
  public void setFromState(State state) {
    cloneStateDataAsNeeded();
    child.stateData.currentMode = state.stateData.currentMode;
    child.stateData.carPickupState = state.stateData.carPickupState;
    child.stateData.vehicleParked = state.stateData.vehicleParked;
    child.stateData.backWalkingBike = state.stateData.backWalkingBike;
  }

  public void setCarPickupState(CarPickupState carPickupState) {
    cloneStateDataAsNeeded();
    child.stateData.carPickupState = carPickupState;
    switch (carPickupState) {
      case WALK_TO_PICKUP, WALK_FROM_DROP_OFF -> child.stateData.currentMode = TraverseMode.WALK;
      case IN_CAR -> child.stateData.currentMode = TraverseMode.CAR;
    }
  }

  public void setTimeSeconds(long seconds) {
    child.time_ms = 1000 * seconds;
  }

  public void setTimeMilliseconds(long milliseconds) {
    child.time_ms = milliseconds;
  }

  /* PUBLIC GETTER METHODS */

  public State getBackState() {
    return child.getBackState();
  }

  public void resetStartedInNoDropOffZone() {
    cloneStateDataAsNeeded();
    child.stateData.noRentalDropOffZonesAtStartOfReverseSearch = Set.of();
  }

  /* PRIVATE METHODS */

  /**
   * To be called before modifying anything in the child's StateData. Makes sure that changes are
   * applied to a copy of StateData rather than the same one that is still referenced in existing,
   * older states.
   */
  private void cloneStateDataAsNeeded() {
    if (child.backState != null && child.stateData == child.backState.stateData) child.stateData =
      child.stateData.clone();
  }
}
