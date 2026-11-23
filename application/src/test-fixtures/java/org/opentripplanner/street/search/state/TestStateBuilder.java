package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.ElevatorHopVertex;
import org.opentripplanner.street.model.vertex.StationEntranceVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * Builds up a state chain for use in tests.
 */
public class TestStateBuilder {

  private static final WgsCoordinate DEFAULT_START = new WgsCoordinate(1, 1);

  private static final Instant DEFAULT_START_TIME = OffsetDateTime.parse(
    "2023-04-18T12:00:00+02:00"
  ).toInstant();
  private int count = 1;

  private State currentState;

  private TestStateBuilder(StreetMode mode) {
    this(mode, AccessEgressType.ACCESS, DEFAULT_START);
  }

  private TestStateBuilder(StreetMode mode, AccessEgressType type, WgsCoordinate start) {
    currentState = new State(
      StreetModelForTest.intersectionVertex(start.latitude(), start.longitude()),
      StreetSearchRequest.of()
        .withArriveBy(type.isEgress())
        .withMode(mode)
        .withStartTime(DEFAULT_START_TIME)
        .build()
    );
  }

  /**
   * Create an initial state that starts walking.
   */
  public static TestStateBuilder ofWalking() {
    return new TestStateBuilder(StreetMode.WALK);
  }

  /**
   * Create an initial state that starts walking in an egress (arriveBy) search, so the state chain
   * runs backward in time.
   */
  public static TestStateBuilder ofWalkingEgress() {
    return new TestStateBuilder(StreetMode.WALK, AccessEgressType.EGRESS, DEFAULT_START);
  }

  /**
   * Create an initial state that starts in a car.
   */
  public static TestStateBuilder ofDriving() {
    return new TestStateBuilder(StreetMode.CAR);
  }

  public static TestStateBuilder ofCarRental() {
    return new TestStateBuilder(StreetMode.CAR_RENTAL);
  }

  public static TestStateBuilder ofScooterRental() {
    return new TestStateBuilder(StreetMode.SCOOTER_RENTAL);
  }

  public static TestStateBuilder ofCycling() {
    return new TestStateBuilder(StreetMode.BIKE);
  }

  public static TestStateBuilder ofBikeAndRide() {
    return new TestStateBuilder(StreetMode.BIKE_TO_PARK);
  }

  public static TestStateBuilder parkAndRide() {
    return new TestStateBuilder(StreetMode.CAR_TO_PARK);
  }

  /**
   * Traverse a very plain street edge with no special characteristics.
   */
  public TestStateBuilder streetEdge() {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelForTest.intersectionVertex(count, count);

    var edge = StreetModelForTest.streetEdge(from, to);
    currentState = requireSingleState(edge.traverse(currentState));
    return this;
  }

  /**
   * Traverse a named street edge. In an arriveBy search the edge leads to the current vertex, since
   * the search runs backward in time.
   */
  public TestStateBuilder streetEdge(String name, int distance_m) {
    count++;
    return streetEdge(name, distance_m, new WgsCoordinate(count, count));
  }

  private TestStateBuilder streetEdge(String name, int distance_m, WgsCoordinate to) {
    var current = (StreetVertex) currentState.vertex;
    var next = StreetModelForTest.intersectionVertex(to.latitude(), to.longitude());
    boolean arriveBy = currentState.getRequest().arriveBy();
    var edge = StreetModelForTest.streetEdgeBuilder(
      arriveBy ? next : current,
      arriveBy ? current : next,
      distance_m,
      StreetTraversalPermission.PEDESTRIAN
    )
      .withName(name)
      .buildAndConnect();

    currentState = requireSingleState(edge.traverse(currentState));
    return this;
  }

  /**
   * Traverse a very plain street edge with stairs with no special characteristics.
   */
  public TestStateBuilder stairsEdge() {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelForTest.intersectionVertex(count, count);
    var edge = StreetModelForTest.streetEdgeBuilder(
      from,
      to,
      30,
      StreetTraversalPermission.PEDESTRIAN,
      true
    ).buildAndConnect();

    currentState = requireSingleState(edge.traverse(currentState));
    return this;
  }

  /**
   * Traverse a very plain escalator edge with no special characteristics.
   */
  public TestStateBuilder escalatorEdge() {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelForTest.intersectionVertex(count, count);
    var edge = StreetModelForTest.escalatorEdge(from, to, 30);

    currentState = requireSingleState(edge.traverse(currentState));
    return this;
  }

  /**
   * Traverse a street edge and switch to Car mode
   */
  public TestStateBuilder pickUpCarFromStation() {
    return pickUpRentalVehicle(
      RentalFormFactor.CAR,
      TestVehicleRentalStationBuilder.of().withVehicleTypeCar(10, 10).build()
    );
  }

  public TestStateBuilder pickUpFreeFloatingScooter() {
    return pickUpRentalVehicle(
      RentalFormFactor.SCOOTER,
      TestFreeFloatingRentalVehicleBuilder.of().withVehicleScooter().build()
    );
  }

  /**
   * Traverse an elevator (onboard, hop and offboard edges).
   */
  public TestStateBuilder elevator() {
    count++;

    var onboard1 = elevator(count, "1");
    var onboard2 = elevator(count, "2");
    var offboard1 = intersection(count);
    var offboard2 = intersection(count);

    var from = (StreetVertex) currentState.vertex;
    var link = StreetModelForTest.streetEdge(from, offboard1);

    var boardEdge = ElevatorBoardEdge.createElevatorBoardEdge(offboard1, onboard1);

    var hopEdge = ElevatorHopEdge.createElevatorHopEdge(
      onboard1,
      onboard2,
      StreetTraversalPermission.PEDESTRIAN,
      Accessibility.POSSIBLE
    );

    var alightEdge = ElevatorAlightEdge.createElevatorAlightEdge(onboard2, offboard2);

    currentState = EdgeTraverser.traverseEdges(
      currentState,
      List.of(link, boardEdge, hopEdge, alightEdge)
    ).orElseThrow();
    return this;
  }

  public TestStateBuilder entrance(String name) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = new StationEntranceVertex(count, count, 12345, "A", Accessibility.POSSIBLE);

    var edge = StreetModelForTest.streetEdgeBuilder(
      from,
      to,
      30,
      StreetTraversalPermission.PEDESTRIAN
    )
      .withName(name)
      .buildAndConnect();
    currentState = requireSingleState(edge.traverse(currentState));
    return this;
  }

  public TestStateBuilder stop(RegularStop stop) {
    return arriveAtStop(stop);
  }

  /**
   * Add a state that arrives at a rental station.
   */
  public TestStateBuilder rentalStation(VehicleRentalStation station) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = new VehicleRentalPlaceVertex(station);

    var link = StreetVehicleRentalLink.createStreetVehicleRentalLink(from, to);
    currentState = requireSingleState(link.traverse(currentState));
    return this;
  }

  private TestStateBuilder arriveAtStop(RegularStop stop) {
    var from = (StreetVertex) currentState.vertex;
    var to = TransitStopVertex.of().withId(stop.getId()).withPoint(stop.getGeometry()).build();

    Edge edge;
    if (currentState.getRequest().arriveBy()) {
      edge = StreetTransitStopLink.createStreetTransitStopLink(to, from);
    } else {
      edge = StreetTransitStopLink.createStreetTransitStopLink(from, to);
    }
    currentState = requireSingleState(edge.traverse(currentState));
    return this;
  }

  private static StreetVertex intersection(int count) {
    return StreetModelForTest.intersectionVertex(count, count);
  }

  private static ElevatorHopVertex elevator(int count, String label) {
    return new ElevatorHopVertex(StreetModelForTest.intersectionVertex(count, count), label);
  }

  private TestStateBuilder pickUpRentalVehicle(
    RentalFormFactor rentalFormFactor,
    VehicleRentalPlace place
  ) {
    count++;
    VehicleRentalPlaceVertex vertex = new VehicleRentalPlaceVertex(place);
    var link = StreetVehicleRentalLink.createStreetVehicleRentalLink(
      (StreetVertex) currentState.vertex,
      vertex
    );
    currentState = link.traverse(currentState)[0];

    var edge = VehicleRentalEdge.createVehicleRentalEdge(vertex, rentalFormFactor);

    State[] traverse = edge.traverse(currentState);
    currentState = Arrays.stream(traverse)
      .filter(it -> it.currentMode() != TraverseMode.WALK)
      .findFirst()
      .get();

    assertTrue(currentState.isRentingVehicle());

    var linkBack = StreetVehicleRentalLink.createStreetVehicleRentalLink(
      (VehicleRentalPlaceVertex) currentState.vertex,
      StreetModelForTest.intersectionVertex(count, count)
    );
    currentState = linkBack.traverse(currentState)[0];

    return this;
  }

  private State requireSingleState(State[] states) {
    if (states.length != 1) {
      throw new IllegalStateException("Only single state transitions are supported.");
    }
    return states[0];
  }

  public State build() {
    return currentState;
  }
}
