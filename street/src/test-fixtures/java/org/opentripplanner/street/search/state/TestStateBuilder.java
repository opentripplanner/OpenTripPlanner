package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.I18NString;
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
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.edge.TestEdge;
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
  private static final WgsCoordinate DEFAULT_START = new WgsCoordinate(1, 1);
  public static final String DEFAULT_STREET_EDGE_NAME = "street edge";
  private int count = 1;

  private State currentState;

  private TestStateBuilder(StreetMode mode) {
    this(mode, DEFAULT_START);
  }

  private TestStateBuilder(StreetMode mode, WgsCoordinate start) {
    this(StreetSearchRequest.of().withMode(mode).withStartTime(DEFAULT_START_TIME).build(), start);
  }

  private TestStateBuilder(StreetSearchRequest request, WgsCoordinate start) {
    currentState = new State(
      StreetModelFactory.intersectionVertex(start.latitude(), start.longitude()),
      request
    );
  }

  public static TestStateBuilder of(StreetSearchRequest request) {
    return new TestStateBuilder(request, DEFAULT_START);
  }

  /**
   * Create an initial state that starts walking.
   */
  public static TestStateBuilder ofWalking() {
    return new TestStateBuilder(StreetMode.WALK);
  }

  /**
   * Create an initial state that starts walking at the given coordinate.
   */
  public static TestStateBuilder ofWalking(WgsCoordinate start) {
    return new TestStateBuilder(StreetMode.WALK, start);
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

  public TestStateBuilder streetEdge() {
    return streetEdge(b -> {});
  }

  /**
   * Traverse a very plain street edge with no special characteristics.
   */
  public TestStateBuilder streetEdge(Consumer<StreetEdgeBuilder<?>> customizer) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelFactory.intersectionVertex(count, count);

    var edgeBuilder = StreetModelFactory.streetEdgeBuilder(
      from,
      to,
      100,
      StreetTraversalPermission.ALL
    );
    customizer.accept(edgeBuilder);
    var edge = edgeBuilder.buildAndConnect();
    var states = edge.traverse(currentState);
    if (states.length != 1) {
      throw new IllegalStateException("Only single state transitions are supported.");
    }
    currentState = states[0];
    return this;
  }

  public TestStateBuilder streetEdge(int distance_m) {
    return streetEdge(DEFAULT_STREET_EDGE_NAME, distance_m);
  }

  public TestStateBuilder streetEdge(int distance_m, WgsCoordinate end) {
    count++;
    return streetEdge(DEFAULT_STREET_EDGE_NAME, distance_m, end);
  }

  public TestStateBuilder streetEdge(String name, int distance_m) {
    count++;
    return streetEdge(name, distance_m, new WgsCoordinate(count, count));
  }

  private TestStateBuilder streetEdge(String name, int distance_m, WgsCoordinate end) {
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelFactory.intersectionVertex(end.latitude(), end.longitude());
    var edge = StreetModelFactory.streetEdgeBuilder(
      from,
      to,
      distance_m,
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

  public TestStateBuilder unnamedStreetEdge(int distance) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelFactory.intersectionVertex(count, count);
    var edge = StreetModelFactory.streetEdgeBuilder(
      from,
      to,
      distance,
      StreetTraversalPermission.PEDESTRIAN
    )
      .withName("path")
      .withNameIsDerived(true)
      .buildAndConnect();

    currentState = requireSingleState(edge.traverse(currentState));
    return this;
  }

  /**
   * Traverse a very plain street edge with stairs with no special characteristics.
   */
  public TestStateBuilder stairsEdge() {
    return streetEdge(b -> b.withPermission(StreetTraversalPermission.PEDESTRIAN).withStairs(true));
  }

  /**
   * Traverse a very plain escalator edge with no special characteristics.
   */
  public TestStateBuilder escalatorEdge() {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelFactory.intersectionVertex(count, count);
    var edge = EscalatorEdge.createEscalatorEdge(from, to, 30, null);
    currentState = edge.traverse(currentState)[0];
    return this;
  }

  /**
   * Traverse an escalator edge ending at a station entrance.
   */
  public TestStateBuilder escalatorEdgeAndStationEntrance() {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = new StationEntranceVertex(count, count, 12345, "B", Accessibility.POSSIBLE);
    var edge = EscalatorEdge.createEscalatorEdge(from, to, 30, null);
    currentState = edge.traverse(currentState)[0];
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

  public TestStateBuilder testEdge() {
    return testEdge(_ -> {});
  }

  public TestStateBuilder testEdge(Consumer<TestEdge.TestEdgeBuilder> customizer) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelFactory.intersectionVertex(count, count);
    var builder = TestEdge.of(from, to);
    customizer.accept(builder);
    var edge = builder.build();
    var states = edge.traverse(currentState);
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

  public TestStateBuilder enterOsmStation(String id) {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = new StationEntranceVertex(count, count, 1, id, Accessibility.NO_INFORMATION);
    var edge = StreetModelFactory.streetEdge(from, to);
    currentState = requireSingleState(edge.traverse(currentState));
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

  public TestStateBuilder pathway(@Nullable String s, double distance_m, WgsCoordinate to) {
    var from = (StreetVertex) currentState.vertex;
    var tov = StreetModelFactory.intersectionVertex(to.latitude(), to.longitude());
    var name = s == null ? null : I18NString.of(s);
    var edge = PathwayEdge.createPathwayEdge(from, tov, name, 0, distance_m, 0, 0, true);
    currentState = requireSingleState(edge.traverse(currentState));
    return this;
  }

  public TestStateBuilder pathway(String s, double distance_m) {
    count++;
    return pathway(s, distance_m, new WgsCoordinate(count, count));
  }

  public TestStateBuilder unnamedPathway(int distance_m) {
    count++;
    return pathway(null, distance_m, new WgsCoordinate(count, count));
  }

  public TestStateBuilder unnamedPathway(int distance_m, WgsCoordinate to) {
    return pathway(null, distance_m, to);
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

  private static State requireSingleState(State[] states) {
    if (states.length != 1) {
      throw new IllegalStateException("Only single state transitions are supported.");
    }
    return states[0];
  }
}
