package org.opentripplanner.astar.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.StreetLocation;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

public class BatteryDistanceSkipEdgeStrategyTest extends GraphRoutingTest {

  /**
   * battery is not enough for driven distance-> skips Edge
   * battery is 0m, driven meters = 100 => true
   */
  @Test
  void batteryIsNotEnough() {
    var vertex = new SimpleVertex(null, -74.01, 40.01);

    var edge = getStreetVehicleRentalLink(0.0, vertex);

    var state = getState(100.0);

    var strategy = new BatteryDistanceSkipEdgeStrategy();

    assertTrue(strategy.shouldSkipEdge(state, edge));
  }

  /**
   * battery is enough for driven distance-> does not skip Edge
   * battery is 4000m, driven meters = 100 => false
   */
  @Test
  void batteryIsEnough() {
    var vertex = new SimpleVertex(null, -74.01, 40.01);

    var edge = getStreetVehicleRentalLink(4000.0, vertex);

    var state = getState(100.0);

    var strategy = new BatteryDistanceSkipEdgeStrategy();
    assertFalse(strategy.shouldSkipEdge(state, edge));
  }

  /**
   * battery dies at exact time location is reached -> does not skip edge
   * battery is 100m, driven meters = 100 => false
   */
  @Test
  void batteryDiesAtFinalLocation() {
    var vertex = new SimpleVertex(null, -74.01, 40.01);

    var edge = getStreetVehicleRentalLink(100.0, vertex);

    var state = getState(100.0);

    var strategy = new BatteryDistanceSkipEdgeStrategy();
    assertFalse(strategy.shouldSkipEdge(state, edge));
  }

  /**
   * battery has remaining Energy, no distance was driven so far -> does not skip edge
   * battery is 100m, driven meters = 0 => false
   */
  @Test
  void noDrivenMeters() {
    var vertex = new SimpleVertex(null, -74.01, 40.01);

    var edge = getStreetVehicleRentalLink(100.0, vertex);

    var state = getState(0.0);

    var strategy = new BatteryDistanceSkipEdgeStrategy();
    assertFalse(strategy.shouldSkipEdge(state, edge));
  }

  /**
   * Edge is of wrong Type (not StreetVehicleLink) -> does not skip Edge
   * edge is no StreetVehicleRentalLink => false
   */
  @Test
  void edgeIsOfWrongType() {
    var from = new StreetLocation(null, new Coordinate(0.0, 0.0), null);
    var to = new StreetLocation(null, new Coordinate(1.0, 1.0), null);
    var edge = FreeEdge.createFreeEdge(from, to);
    var state = TestStateBuilder.ofScooterRental().build();
    var strategy = new BatteryDistanceSkipEdgeStrategy();
    assertFalse(strategy.shouldSkipEdge(state, edge));
  }

  /**
   * vehicle.currentRangeMeters (battery) is Null or of Empty Value -> does not skip Edge
   * Battery is Optional.Empty() => false
   */
  @Test
  void batteryHasNoValue() {
    var vertex = new SimpleVertex(null, -74.01, 40.01);

    var vehicle = new VehicleRentalVehicle();
    vehicle.currentRangeMeters = null;

    var rentalVertex = new VehicleRentalPlaceVertex(vehicle);

    var edge = StreetVehicleRentalLink.createStreetVehicleRentalLink(vertex, rentalVertex);

    var state = TestStateBuilder.ofScooterRental().build();

    var strategy = new BatteryDistanceSkipEdgeStrategy();
    assertFalse(strategy.shouldSkipEdge(state, edge));
  }

  private static StreetVehicleRentalLink getStreetVehicleRentalLink(
    double currentRangeMeters,
    SimpleVertex vertex
  ) {
    var vehicle = new VehicleRentalVehicle();
    vehicle.currentRangeMeters = currentRangeMeters;
    var rentalVertex = new VehicleRentalPlaceVertex(vehicle);
    var edge = StreetVehicleRentalLink.createStreetVehicleRentalLink(vertex, rentalVertex);
    return edge;
  }

  private static State getState(double batteryDistance) {
    var state = TestStateBuilder.ofScooterRental().build();
    state.batteryDistance = batteryDistance;
    return state;
  }
}
