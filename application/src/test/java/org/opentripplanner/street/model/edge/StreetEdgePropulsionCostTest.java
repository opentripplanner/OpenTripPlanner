package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * Tests for propulsion-aware cost calculation in StreetEdge.
 * <p>
 * Verifies that different propulsion types (ELECTRIC, ELECTRIC_ASSIST, HUMAN) result in
 * appropriate cost calculations, especially regarding slope effects:
 * - ELECTRIC (e-scooters): Use flat distance (constant speed, motor does all work)
 * - ELECTRIC_ASSIST (e-bikes): Reduced slope sensitivity (default 30% of human-powered effect)
 * - HUMAN and others: Full slope effect
 */
class StreetEdgePropulsionCostTest {

  private static final double DELTA = 0.00001;
  private static final double SPEED = 6.0;
  private static final double LENGTH = 650.0;

  private final StreetVertex v0 = intersectionVertex(0.0, 0.0);
  private final StreetVertex v1 = intersectionVertex(2.0, 2.0);

  private StreetVertex from;
  private StreetVertex to;
  private StreetEdge hillyEdge;
  private double flatDistance;
  private double slopedDistance;

  @BeforeEach
  void setUp() {
    Coordinate c1 = new Coordinate(-122.575033, 45.456773);
    Coordinate c2 = new Coordinate(-122.576668, 45.451426);

    from = intersectionVertex("from", c1.y, c1.x);
    to = intersectionVertex("to", c2.y, c2.x);

    var geometry =
      org.opentripplanner.framework.geometry.GeometryUtils.getGeometryFactory().createLineString(
        new Coordinate[] { c1, c2 }
      );

    hillyEdge = new StreetEdgeBuilder<>()
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

    flatDistance = hillyEdge.getDistanceMeters();
    slopedDistance = hillyEdge.getEffectiveBikeDistance();
  }

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

  @Test
  void hillyEdgeHasSlopeEffect() {
    // Verify that the test edge has slope effect
    assertTrue(slopedDistance > flatDistance);
  }

  /**
   * Test cases for propulsion type slope sensitivity.
   * Each case specifies: propulsion type, form factor, street mode, slope sensitivity factor.
   * <p>
   * Slope sensitivity determines how much the elevation profile affects travel time:
   * - 0.0: No slope effect (electric scooters maintain constant speed)
   * - 0.3: Default e-assist sensitivity (motor helps on hills)
   * - 1.0: Full slope effect (human-powered)
   */
  static Stream<Arguments> propulsionSlopeCases() {
    return Stream.of(
      of(PropulsionType.ELECTRIC, RentalFormFactor.SCOOTER, StreetMode.SCOOTER_RENTAL, 0.0),
      of(
        PropulsionType.ELECTRIC_ASSIST,
        RentalFormFactor.BICYCLE,
        StreetMode.BIKE_RENTAL,
        VehicleRentalPreferences.DEFAULT_ELECTRIC_ASSIST_SLOPE_SENSITIVITY
      ),
      of(PropulsionType.HUMAN, RentalFormFactor.BICYCLE, StreetMode.BIKE_RENTAL, 1.0)
    );
  }

  /**
   * Tests that different propulsion types result in appropriate slope sensitivity for time
   * calculation. Uses TRIANGLE optimization with time=1 to isolate the time/speed component.
   */
  @ParameterizedTest(
    name = "propulsion={0}, formFactor={1}, mode={2} should have slope sensitivity {3}"
  )
  @MethodSource("propulsionSlopeCases")
  void propulsionTypeAffectsSlopeCalculation(
    PropulsionType propulsionType,
    RentalFormFactor formFactor,
    StreetMode streetMode,
    double slopeSensitivity
  ) {
    double expectedEffectiveDistance =
      flatDistance + (slopedDistance - flatDistance) * slopeSensitivity;
    double expectedWeight = expectedEffectiveDistance / SPEED;

    State state = createRentalState(streetMode, formFactor, propulsionType);
    var result = hillyEdge.traverse(state)[0];

    assertEquals(expectedWeight, result.getWeight() - state.getWeight(), DELTA);
  }

  /**
   * Tests that a custom electric-assist slope sensitivity is honored.
   */
  @Test
  void customElectricAssistSlopeSensitivity() {
    double customSensitivity = 0.5;
    double expectedEffectiveDistance =
      flatDistance + (slopedDistance - flatDistance) * customSensitivity;
    double expectedWeight = expectedEffectiveDistance / SPEED;

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

    assertEquals(expectedWeight, result.getWeight() - state.getWeight(), DELTA);
  }

  private State createRentalState(
    StreetMode streetMode,
    RentalFormFactor formFactor,
    PropulsionType propulsionType
  ) {
    StreetSearchRequest req;
    if (streetMode == StreetMode.SCOOTER_RENTAL) {
      req = StreetSearchRequest.of()
        .withMode(streetMode)
        .withScooter(scooter ->
          scooter
            .withSpeed(SPEED)
            .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
            .withOptimizeTriangle(it -> it.withTime(1))
            .withReluctance(1)
        )
        .build();
    } else {
      req = StreetSearchRequest.of()
        .withMode(streetMode)
        .withBike(bike ->
          bike
            .withSpeed(SPEED)
            .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
            .withOptimizeTriangle(it -> it.withTime(1))
            .withReluctance(1)
        )
        .build();
    }

    var editor = new StateEditor(from, req);
    editor.beginFloatingVehicleRenting(formFactor, propulsionType, "network", false);
    return editor.makeState();
  }
}
