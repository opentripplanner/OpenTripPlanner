package org.opentripplanner.street.search.state;

import static org.opentripplanner.utils.lang.ObjectUtils.requireNotInitialized;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class State implements AStarState<State, Edge, Vertex>, Cloneable {

  private static final State[] EMPTY_STATES = {};
  private final StreetSearchRequest request;

  /* Data which is likely to change at most traversals */

  // the current time at this state, in milliseconds since UNIX epoch
  protected long time_ms;

  // accumulated weight up to this state
  public double weight;

  // associate this state with a vertex in the graph
  protected Vertex vertex;

  // allow path reconstruction from states
  protected State backState;

  public Edge backEdge;

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
  public State(Vertex vertex, StreetSearchRequest streetSearchRequest) {
    this(
      vertex,
      streetSearchRequest.startTime(),
      StateData.getBaseCaseStateData(streetSearchRequest),
      streetSearchRequest
    );
  }

  public State(Vertex vertex, Instant startTime, StateData stateData, StreetSearchRequest request) {
    this.request = request;
    this.weight = 0;
    this.vertex = vertex;
    this.backState = null;
    this.stateData = stateData;
    if (request.arriveBy() && !vertex.rentalRestrictions().noDropOffNetworks().isEmpty()) {
      this.stateData.noRentalDropOffZonesAtStartOfReverseSearch =
        vertex.rentalRestrictions().noDropOffNetworks();
    }
    this.walkDistance = 0;
    this.time_ms = startTime.toEpochMilli();
  }

  /**
   * Create an initial state representing the beginning of a search for the given routing request.
   * Initial "parent-less" states can only be created at the beginning of a trip. elsewhere, all
   * states must be created from a parent and associated with an edge.
   */
  public static Collection<State> getInitialStates(
    Set<Vertex> vertices,
    StreetSearchRequest streetSearchRequest
  ) {
    Collection<State> states = new ArrayList<>();
    for (Vertex vertex : vertices) {
      for (StateData stateData : StateData.getInitialStateDatas(streetSearchRequest)) {
        states.add(
          new State(vertex, streetSearchRequest.startTime(), stateData, streetSearchRequest)
        );
      }
    }
    return states;
  }

  /**
   * Takes a nullable state and returns an array of states, possibly empty.
   */
  public static State[] ofNullable(@Nullable State u) {
    if (u == null) {
      return EMPTY_STATES;
    } else {
      return new State[] { u };
    }
  }

  /**
   * Takes two nullable states and returns an array of states (possibly empty) which is guaranteed
   * to contain no nulls.
   * <p>
   * This method is optimized for a low number of allocations and therefore doesn't use any streams
   * or collections to filter out the nulls.
   */
  public static State[] ofNullable(@Nullable State s1, @Nullable State s2) {
    if (s1 == null && s2 == null) {
      return EMPTY_STATES;
    } else if (s1 == null) {
      return new State[] { s2 };
    } else if (s2 == null) {
      return new State[] { s1 };
    } else {
      return new State[] { s1, s2 };
    }
  }

  /**
   * Convenience method to return an empty array of states.
   */
  public static State[] empty() {
    return EMPTY_STATES;
  }

  /**
   * Convenience method to check if the state array is empty.
   */
  public static boolean isEmpty(State[] s) {
    return s.length == 0;
  }

  /**
   * Takes a stream of states and converts it to an array while removing nulls.
   */
  public static State[] ofStream(Stream<State> states) {
    return states.filter(Objects::nonNull).toArray(State[]::new);
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

  /** Always round the same way and in the same direction when converting milliseconds to seconds.
   * This means that request.arriveBy must be taken into account. Used in many places. */
  private long millisecondsToSeconds(long milliseconds) {
    if (request.arriveBy()) {
      return milliseconds / 1000L;
    } else {
      return (milliseconds + 999L) / 1000L;
    }
  }

  /** Returns time in seconds since epoch */
  public long getTimeSeconds() {
    return millisecondsToSeconds(time_ms);
  }

  public long getTimeMilliseconds() {
    return time_ms;
  }

  /** returns the length of the trip in seconds up to this state */
  public long getElapsedTimeSeconds() {
    return (getElapsedTimeMilliseconds() + 999L) / 1000L;
  }

  public long getElapsedTimeMilliseconds() {
    return Math.abs(getTimeMilliseconds() - request.startTime().toEpochMilli());
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

  private boolean vehicleRentalIsFinished() {
    return (
      stateData.vehicleRentalState == VehicleRentalState.HAVE_RENTED ||
      (
        stateData.vehicleRentalState == VehicleRentalState.RENTING_FLOATING &&
        !stateData.insideNoRentalDropOffArea
      ) ||
      (
        getRequest()
          .preferences()
          .rental(getRequest().mode())
          .allowArrivingInRentedVehicleAtDestination() &&
        stateData.mayKeepRentedVehicleAtDestination &&
        stateData.vehicleRentalState == VehicleRentalState.RENTING_FROM_STATION
      )
    );
  }

  private boolean vehicleRentalNotStarted() {
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

  public RentalFormFactor vehicleRentalFormFactor() {
    return stateData.rentalVehicleFormFactor;
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
    return (int) millisecondsToSeconds(getTimeDeltaMilliseconds());
  }

  public int getTimeDeltaMilliseconds() {
    return backState != null ? (int) (getTimeMilliseconds() - backState.getTimeMilliseconds()) : 0;
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

  public void initBackEdge(Edge initialBackEdge) {
    this.backEdge = requireNotInitialized(this.backEdge, initialBackEdge);
  }

  public StreetSearchRequest getRequest() {
    return request;
  }

  public RoutingPreferences getPreferences() {
    return request.preferences();
  }

  /**
   * @return The current mode of this state. When doing a rental request, this can for example
   * indicate if the state is currently using a vehicle or not.
   */
  public TraverseMode currentMode() {
    return stateData.currentMode;
  }

  public Instant getTime() {
    // We're not letting the subsecond time out right now, because everything else
    // expects whole seconds.
    return Instant.ofEpochSecond(millisecondsToSeconds(time_ms));
  }

  public Instant getTimeAccurate() {
    return Instant.ofEpochMilli(time_ms);
  }

  public String getVehicleRentalNetwork() {
    return stateData.vehicleRentalNetwork;
  }

  /**
   * Whether we know or don't know the rental network (yet).
   * <p>
   * When doing a arriveBy search it is possible to be in a renting state without knowing which
   * network it is.
   */
  public boolean unknownRentalNetwork() {
    return stateData.vehicleRentalNetwork == null;
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

      editor.incrementTimeInMilliseconds(orig.getAbsTimeDeltaMilliseconds());
      editor.incrementWeight(orig.getWeightDelta());
      editor.incrementWalkDistance(orig.getWalkDistanceDelta());

      // propagate the modes through to the reversed edge
      editor.setBackMode(orig.getBackMode());

      if (orig.isRentingVehicle() && !orig.getBackState().isRentingVehicle()) {
        if (orig.vertex instanceof VehicleRentalPlaceVertex stationVertex) {
          editor.dropOffRentedVehicleAtStation(
            ((VehicleRentalEdge) edge).formFactor,
            stationVertex.getStation().getNetwork(),
            false
          );
        } else {
          editor.dropFloatingVehicle(
            orig.stateData.rentalVehicleFormFactor,
            orig.getVehicleRentalNetwork(),
            false
          );
        }
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
        editor.setVehicleParked(true, orig.getBackState().currentMode());
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
    return request.intersectionTraversalCalculator();
  }

  public DataOverlayContext dataOverlayContext() {
    return request.dataOverlayContext();
  }

  public boolean isInsideNoRentalDropOffArea() {
    return stateData.insideNoRentalDropOffArea;
  }

  /**
   * Whether the street path contains any driving.
   */
  public boolean containsModeCar() {
    var state = this;
    while (state != null) {
      if (state.currentMode().isInCar()) {
        return true;
      } else {
        state = state.getBackState();
      }
    }
    return false;
  }

  /**
   * Check all edges is traversed on foot {@code mode=WALK}.
   * <p>
   * DO NOT USE THIS IN ROUTING IT WILL NOT PERFORM WELL!
   */
  public boolean containsOnlyWalkMode() {
    // The for-loop has the best performance
    for (var s = this; s != null; s = s.backState) {
      if (!s.stateData.currentMode.isWalking()) {
        return false;
      }
    }
    return true;
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
      .addEnum("formFactor", vehicleRentalFormFactor())
      .addBoolIfTrue("RENTING_FROM_STATION", isRentingVehicleFromStation())
      .addBoolIfTrue(
        "RENTING_FREE_FLOATING",
        isRentingFloatingVehicle() && !isRentingVehicleFromStation()
      )
      .addBoolIfTrue("VEHICLE_PARKED", isVehicleParked())
      .toString();
  }

  void checkNegativeWeight() {
    double dw = this.weight - backState.weight;
    if (dw < 0) {
      throw new NegativeWeightException(dw + " on edge " + backEdge);
    }
  }

  private int getAbsTimeDeltaMilliseconds() {
    return Math.abs(getTimeDeltaMilliseconds());
  }

  private double getWalkDistanceDelta() {
    if (backState != null) {
      return Math.abs(this.walkDistance - backState.walkDistance);
    } else {
      return 0.0;
    }
  }

  private State reversedClone() {
    StreetSearchRequest reversedRequest = request
      .copyOfReversed(getTime())
      .withPreferences(p -> {
        p.withCar(c -> c.withRental(r -> r.withUseAvailabilityInformation(false)));
        p.withBike(b -> b.withRental(r -> r.withUseAvailabilityInformation(false)));
      })
      .build();
    StateData newStateData = stateData.clone();
    newStateData.backMode = null;
    return new State(this.vertex, getTime(), newStateData, reversedRequest);
  }

  /**
   * This exception is thrown when an edge has a negative weight. Dijkstra's algorithm (and A*) don't
   * work on graphs that have negative weights.  This exception almost always indicates a programming
   * error, but could be caused by bad GTFS data.
   */
  private static class NegativeWeightException extends RuntimeException {

    public NegativeWeightException(String message) {
      super(message);
    }
  }
}
