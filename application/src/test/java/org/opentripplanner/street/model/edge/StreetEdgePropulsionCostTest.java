package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType.PropulsionType;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * Tests for propulsion-aware cost calculation in StreetEdge.
 * <p>
 * Verifies that different propulsion types (ELECTRIC, ELECTRIC_ASSIST, HUMAN) result in
 * appropriate cost calculations, especially regarding slope effects:
 * - ELECTRIC (e-scooters): Use flat distance (constant speed, motor does all work)
 * - ELECTRIC_ASSIST (e-bikes): Reduced slope sensitivity (30% of human-powered effect)
 * - HUMAN and others: Full slope effect
 */
class StreetEdgePropulsionCostTest {

  private static final double DELTA = 0.00001;
  private static final double SPEED = 6.0;
  private static final double LENGTH = 650.0;

  private final StreetVertex v0 = intersectionVertex(0.0, 0.0);
  private final StreetVertex v1 = intersectionVertex(2.0, 2.0);

  @Test
  void rentalScooterCanTraverseEdge() {
    StreetEdge edge = streetEdge(v0, v1, 100.0, StreetTraversalPermission.ALL);
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var editor = new StateEditor(v0, req);
    editor.beginFloatingVehicleRenting(
      RentalFormFactor.SCOOTER,
      PropulsionType.ELECTRIC,
      "network",
      false
    );
    var state = editor.makeState();

    var result = edge.traverse(state);
    assertNotNull(result);
    assertEquals(1, result.length);
    assertEquals(PropulsionType.ELECTRIC, result[0].rentalVehiclePropulsionType());
  }

  /**
   * Tests that electric scooters ignore slope for time calculation.
   * On a hilly street with elevation profile, an electric scooter should use flat distance
   * for its time estimate since the motor maintains constant speed.
   * <p>
   * Uses TRIANGLE optimization with time=1 to isolate the time/speed component,
   * similar to the approach in StreetEdgeScooterTraversalTest.
   */
  @Test
  void electricScooterUsesConstantSpeedOnHillyTerrain() {
    // Create edge with elevation profile
    Coordinate c1 = new Coordinate(-122.575033, 45.456773);
    Coordinate c2 = new Coordinate(-122.576668, 45.451426);

    StreetVertex from = intersectionVertex("from", c1.y, c1.x);
    StreetVertex to = intersectionVertex("to", c2.y, c2.x);

    var geometry = org.opentripplanner.framework.geometry.GeometryUtils.getGeometryFactory()
      .createLineString(new Coordinate[] { c1, c2 });

    StreetEdge hillyEdge = new StreetEdgeBuilder<>()
      .withFromVertex(from)
      .withToVertex(to)
      .withGeometry(geometry)
      .withName("Hilly Street")
      .withMeterLength(LENGTH)
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();

    // Add elevation profile (10% slope up and down)
    Coordinate[] profile = new Coordinate[] {
      new Coordinate(0, 0),
      new Coordinate(LENGTH / 2, LENGTH / 20.0), // 10% slope up
      new Coordinate(LENGTH, 0), // 10% slope down
    };
    PackedCoordinateSequence elev = new PackedCoordinateSequence.Double(profile);
    StreetElevationExtensionBuilder.of(hillyEdge)
      .withElevationProfile(elev)
      .withComputed(false)
      .build()
      .ifPresent(hillyEdge::setElevationExtension);

    // Verify the edge has slope effect (effective bike distance should differ from flat distance)
    double effectiveBikeDistance = hillyEdge.getEffectiveBikeDistance();
    double flatDistance = hillyEdge.getDistanceMeters();
    // The effective bike distance should be greater than flat due to uphill slope penalty
    assertTrue(effectiveBikeDistance > flatDistance);

    // Electric scooter rental - use TRIANGLE with time=1 to test time calculation
    var req = StreetSearchRequest.of()
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withScooter(scooter ->
        scooter
          .withSpeed(SPEED)
          .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
          .withOptimizeTriangle(it -> it.withTime(1))
          .withReluctance(1)
      )
      .build();

    var editor = new StateEditor(from, req);
    editor.beginFloatingVehicleRenting(
      RentalFormFactor.SCOOTER,
      PropulsionType.ELECTRIC,
      "network",
      false
    );
    var state = editor.makeState();

    var result = hillyEdge.traverse(state)[0];

    // Electric scooter should use flat distance for time (ignores slope)
    double expectedWeight = LENGTH / SPEED; // flat distance / speed
    assertEquals(expectedWeight, result.getWeight() - state.getWeight(), DELTA);
  }

  /**
   * Tests that human-powered bikes use full slope effect for time calculation.
   * Uses TRIANGLE optimization with time=1 to isolate the time/speed component.
   */
  @Test
  void humanPoweredBikeUsesFullSlopeEffect() {
    // Create edge with elevation profile
    Coordinate c1 = new Coordinate(-122.575033, 45.456773);
    Coordinate c2 = new Coordinate(-122.576668, 45.451426);

    StreetVertex from = intersectionVertex("from", c1.y, c1.x);
    StreetVertex to = intersectionVertex("to", c2.y, c2.x);

    var geometry = org.opentripplanner.framework.geometry.GeometryUtils.getGeometryFactory()
      .createLineString(new Coordinate[] { c1, c2 });

    StreetEdge hillyEdge = new StreetEdgeBuilder<>()
      .withFromVertex(from)
      .withToVertex(to)
      .withGeometry(geometry)
      .withName("Hilly Street")
      .withMeterLength(LENGTH)
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();

    // Add elevation profile (10% slope up and down)
    Coordinate[] profile = new Coordinate[] {
      new Coordinate(0, 0),
      new Coordinate(LENGTH / 2, LENGTH / 20.0),
      new Coordinate(LENGTH, 0),
    };
    PackedCoordinateSequence elev = new PackedCoordinateSequence.Double(profile);
    StreetElevationExtensionBuilder.of(hillyEdge)
      .withElevationProfile(elev)
      .withComputed(false)
      .build()
      .ifPresent(hillyEdge::setElevationExtension);

    double slopedDistance = hillyEdge.getEffectiveBikeDistance();

    // Human-powered bike rental - use TRIANGLE with time=1 to test time calculation
    var req = StreetSearchRequest.of()
      .withMode(StreetMode.BIKE_RENTAL)
      .withBike(bike ->
        bike
          .withSpeed(SPEED)
          .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
          .withOptimizeTriangle(it -> it.withTime(1))
          .withReluctance(1)
      )
      .build();

    var editor = new StateEditor(from, req);
    editor.beginFloatingVehicleRenting(
      RentalFormFactor.BICYCLE,
      PropulsionType.HUMAN,
      "network",
      false
    );
    var state = editor.makeState();

    var result = hillyEdge.traverse(state)[0];

    // Human-powered bike should use sloped distance for time (full slope effect)
    double expectedWeight = slopedDistance / SPEED;
    assertEquals(expectedWeight, result.getWeight() - state.getWeight(), DELTA);
  }

  /**
   * Tests that electric-assist bikes have reduced slope sensitivity (30%).
   * Uses TRIANGLE optimization with time=1 to isolate the time/speed component.
   */
  @Test
  void electricAssistBikeHasReducedSlopeEffect() {
    // Create edge with elevation profile
    Coordinate c1 = new Coordinate(-122.575033, 45.456773);
    Coordinate c2 = new Coordinate(-122.576668, 45.451426);

    StreetVertex from = intersectionVertex("from", c1.y, c1.x);
    StreetVertex to = intersectionVertex("to", c2.y, c2.x);

    var geometry = org.opentripplanner.framework.geometry.GeometryUtils.getGeometryFactory()
      .createLineString(new Coordinate[] { c1, c2 });

    StreetEdge hillyEdge = new StreetEdgeBuilder<>()
      .withFromVertex(from)
      .withToVertex(to)
      .withGeometry(geometry)
      .withName("Hilly Street")
      .withMeterLength(LENGTH)
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();

    // Add elevation profile (10% slope up and down)
    Coordinate[] profile = new Coordinate[] {
      new Coordinate(0, 0),
      new Coordinate(LENGTH / 2, LENGTH / 20.0),
      new Coordinate(LENGTH, 0),
    };
    PackedCoordinateSequence elev = new PackedCoordinateSequence.Double(profile);
    StreetElevationExtensionBuilder.of(hillyEdge)
      .withElevationProfile(elev)
      .withComputed(false)
      .build()
      .ifPresent(hillyEdge::setElevationExtension);

    double flatDistance = hillyEdge.getDistanceMeters();
    double slopedDistance = hillyEdge.getEffectiveBikeDistance();
    // Use default slope sensitivity from preferences
    double slopeSensitivity = VehicleRentalPreferences.DEFAULT_ELECTRIC_ASSIST_SLOPE_SENSITIVITY;
    double expectedEffectiveDistance =
      flatDistance + (slopedDistance - flatDistance) * slopeSensitivity;

    // Electric-assist bike rental - use TRIANGLE with time=1 to test time calculation
    var req = StreetSearchRequest.of()
      .withMode(StreetMode.BIKE_RENTAL)
      .withBike(bike ->
        bike
          .withSpeed(SPEED)
          .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
          .withOptimizeTriangle(it -> it.withTime(1))
          .withReluctance(1)
      )
      .build();

    var editor = new StateEditor(from, req);
    editor.beginFloatingVehicleRenting(
      RentalFormFactor.BICYCLE,
      PropulsionType.ELECTRIC_ASSIST,
      "network",
      false
    );
    var state = editor.makeState();

    var result = hillyEdge.traverse(state)[0];

    // Electric-assist bike should use 30% of slope effect
    double expectedWeight = expectedEffectiveDistance / SPEED;
    assertEquals(expectedWeight, result.getWeight() - state.getWeight(), DELTA);
  }

  /**
   * Tests that a custom electric-assist slope sensitivity is honored.
   */
  @Test
  void customElectricAssistSlopeSensitivity() {
    // Create edge with elevation profile
    Coordinate c1 = new Coordinate(-122.575033, 45.456773);
    Coordinate c2 = new Coordinate(-122.576668, 45.451426);

    StreetVertex from = intersectionVertex("from", c1.y, c1.x);
    StreetVertex to = intersectionVertex("to", c2.y, c2.x);

    var geometry = org.opentripplanner.framework.geometry.GeometryUtils.getGeometryFactory()
      .createLineString(new Coordinate[] { c1, c2 });

    StreetEdge hillyEdge = new StreetEdgeBuilder<>()
      .withFromVertex(from)
      .withToVertex(to)
      .withGeometry(geometry)
      .withName("Hilly Street")
      .withMeterLength(LENGTH)
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();

    // Add elevation profile
    Coordinate[] profile = new Coordinate[] {
      new Coordinate(0, 0),
      new Coordinate(LENGTH / 2, LENGTH / 20.0),
      new Coordinate(LENGTH, 0),
    };
    PackedCoordinateSequence elev = new PackedCoordinateSequence.Double(profile);
    StreetElevationExtensionBuilder.of(hillyEdge)
      .withElevationProfile(elev)
      .withComputed(false)
      .build()
      .ifPresent(hillyEdge::setElevationExtension);

    double flatDistance = hillyEdge.getDistanceMeters();
    double slopedDistance = hillyEdge.getEffectiveBikeDistance();

    // Use custom sensitivity of 0.5 (50% slope effect)
    double customSensitivity = 0.5;
    double expectedEffectiveDistance =
      flatDistance + (slopedDistance - flatDistance) * customSensitivity;

    // Electric-assist bike rental with custom slope sensitivity
    var req = StreetSearchRequest.of()
      .withMode(StreetMode.BIKE_RENTAL)
      .withBike(bike ->
        bike
          .withSpeed(SPEED)
          .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
          .withOptimizeTriangle(it -> it.withTime(1))
          .withReluctance(1)
          .withRental(rental -> rental.withElectricAssistSlopeSensitivity(customSensitivity))
      )
      .build();

    var editor = new StateEditor(from, req);
    editor.beginFloatingVehicleRenting(
      RentalFormFactor.BICYCLE,
      PropulsionType.ELECTRIC_ASSIST,
      "network",
      false
    );
    var state = editor.makeState();

    var result = hillyEdge.traverse(state)[0];

    double expectedWeight = expectedEffectiveDistance / SPEED;
    assertEquals(expectedWeight, result.getWeight() - state.getWeight(), DELTA);
  }
}
