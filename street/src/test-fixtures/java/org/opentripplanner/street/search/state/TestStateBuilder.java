package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.vertex.ElevatorHopVertex;
import org.opentripplanner.street.model.vertex.StationEntranceVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

/**
 * Builds up a state chain for use in tests.
 */
public class TestStateBuilder {

  private static final Instant DEFAULT_START_TIME = OffsetDateTime.parse(
    "2023-04-18T12:00:00+02:00"
  ).toInstant();
  private int count = 1;

  private State currentState;

  private TestStateBuilder(StreetMode mode) {
    currentState = new State(
      StreetModelFactory.intersectionVertex(count, count),
      StreetSearchRequest.of().withMode(mode).withStartTime(DEFAULT_START_TIME).build()
    );
  }

  /**
   * Create an initial state that starts walking.
   */
  public static TestStateBuilder ofWalking() {
    return new TestStateBuilder(StreetMode.WALK);
  }

  /**
   * Create an initial state that start in a car.
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

  public static TestStateBuilder ofBikeRental() {
    return new TestStateBuilder(StreetMode.BIKE_RENTAL);
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
    var to = StreetModelFactory.intersectionVertex(count, count);

    var edge = StreetModelFactory.streetEdge(from, to);
    var states = edge.traverse(currentState);
    if (states.length != 1) {
      throw new IllegalStateException("Only single state transitions are supported.");
    }
    currentState = states[0];
    return this;
  }

  public TestStateBuilder streetEdge(String name, int distance) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelFactory.intersectionVertex(count, count);
    var edge = StreetModelFactory.streetEdgeBuilder(
      from,
      to,
      distance,
      StreetTraversalPermission.PEDESTRIAN
    )
      .withName(name)
      .buildAndConnect();

    var states = edge.traverse(currentState);
    if (states.length != 1) {
      throw new IllegalStateException("Only single state transitions are supported.");
    }
    currentState = states[0];
    return this;
  }

  public TestStateBuilder areaEdge(String name, int distance) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelFactory.intersectionVertex(count, count);
    var area = StreetModelFactory.areaEdge(from, to, name, StreetTraversalPermission.PEDESTRIAN);
    var states = area.traverse(currentState);
    if (states.length != 1) {
      throw new IllegalStateException("Only single state transitions are supported.");
    }
    currentState = states[0];
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

  public TestStateBuilder pickUpFreeFloatingCar() {
    return pickUpRentalVehicle(
      RentalFormFactor.CAR,
      TestFreeFloatingRentalVehicleBuilder.of().withVehicleCar().build()
    );
  }

  public TestStateBuilder pickUpFreeFloatingScooter() {
    return pickUpRentalVehicle(
      RentalFormFactor.SCOOTER,
      TestFreeFloatingRentalVehicleBuilder.of().withVehicleScooter().build()
    );
  }

  public TestStateBuilder pickUpBikeFromStation() {
    return pickUpRentalVehicle(
      RentalFormFactor.BICYCLE,
      TestVehicleRentalStationBuilder.of().withVehicleTypeElectricBicycle(10, 10).build()
    );
  }

  public TestStateBuilder pickUpFreeFloatingBike() {
    return pickUpRentalVehicle(
      RentalFormFactor.BICYCLE,
      TestFreeFloatingRentalVehicleBuilder.of().withVehicleBicycle().build()
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
    var link = StreetModelFactory.streetEdge(from, offboard1);

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

    var edge = StreetModelFactory.streetEdgeBuilder(
      from,
      to,
      30,
      StreetTraversalPermission.PEDESTRIAN
    )
      .withName(name)
      .buildAndConnect();
    currentState = edge.traverse(currentState)[0];
    return this;
  }

  /**
   * Add a state that arrives at a rental station.
   */
  public TestStateBuilder rentalStation(VehicleRentalStation station) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = new VehicleRentalPlaceVertex(station);

    var link = StreetVehicleRentalLink.createStreetVehicleRentalLink(from, to);
    currentState = link.traverse(currentState)[0];
    return this;
  }

  public TestStateBuilder enterStation(String id) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    final var entranceVertex = StreetModelFactory.transitEntranceVertex(id, count, count);
    var edge = StreetTransitEntranceLink.createStreetTransitEntranceLink(from, entranceVertex);
    var states = edge.traverse(currentState);
    currentState = states[0];
    return this;
  }

  public TestStateBuilder exitStation(String id) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var entranceVertex = StreetModelFactory.transitEntranceVertex(id, count, count);
    var edge = PathwayEdge.createLowCostPathwayEdge(from, entranceVertex, true);
    var state = edge.traverse(currentState)[0];

    count++;
    var to = StreetModelFactory.intersectionVertex(count, count);
    var link = StreetTransitEntranceLink.createStreetTransitEntranceLink(entranceVertex, to);
    var states = link.traverse(state);
    currentState = states[0];
    return this;
  }

  public TestStateBuilder pathway(String s) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var tov = StreetModelFactory.intersectionVertex(count, count);
    var edge = PathwayEdge.createPathwayEdge(from, tov, I18NString.of(s), 60, 100, 0, 0, true);
    currentState = edge.traverse(currentState)[0];
    return this;
  }

  private static StreetVertex intersection(int count) {
    return StreetModelFactory.intersectionVertex(count, count);
  }

  private static ElevatorHopVertex elevator(int count, String label) {
    return new ElevatorHopVertex(StreetModelFactory.intersectionVertex(count, count), label);
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
      StreetModelFactory.intersectionVertex(count, count)
    );
    currentState = linkBack.traverse(currentState)[0];

    return this;
  }

  public State build() {
    return currentState;
  }
}
