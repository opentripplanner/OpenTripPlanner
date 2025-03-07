package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.util.SlopeCosts;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

public class StreetEdgeScooterTraversalTest {

  private static final double DELTA = 0.00001;
  private static final double SPEED = 6.0;

  @Test
  public void testTraverseFloatingScooter() {
    // This test does not depend on the setup method - and can probably be simplified
    Coordinate c1 = new Coordinate(-122.575033, 45.456773);
    Coordinate c2 = new Coordinate(-122.576668, 45.451426);

    var formFactor = RentalFormFactor.SCOOTER;
    var rentalVertex = StreetModelForTest.rentalVertex(formFactor);
    var vehicleRentalEdge = VehicleRentalEdge.createVehicleRentalEdge(rentalVertex, formFactor);

    StreetVertex v1 = StreetModelForTest.intersectionVertex("v1", c1.x, c1.y);
    StreetVertex v2 = StreetModelForTest.intersectionVertex("v2", c2.x, c2.y);

    var link = StreetVehicleRentalLink.createStreetVehicleRentalLink(rentalVertex, v1);

    GeometryFactory factory = new GeometryFactory();
    LineString geometry = factory.createLineString(new Coordinate[] { c1, c2 });

    double length = 650.0;

    StreetEdge testStreet = new StreetEdgeBuilder<>()
      .withFromVertex(v1)
      .withToVertex(v2)
      .withGeometry(geometry)
      .withName("Test Lane")
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();

    var request = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL);

    request.withPreferences(pref -> pref.withScooter(scooter -> scooter.withSpeed(5)));

    State slowResult = traverseStreetFromRental(
      testStreet,
      vehicleRentalEdge,
      link,
      rentalVertex,
      request.build()
    );
    request.withPreferences(pref -> pref.withScooter(scooter -> scooter.withSpeed(10)));

    State fastResult = traverseStreetFromRental(
      testStreet,
      vehicleRentalEdge,
      link,
      rentalVertex,
      request.build()
    );

    // Cost and time should be less when scooter speed is higher.
    assertTrue(slowResult.getWeight() > fastResult.getWeight() + DELTA);
    assertTrue(slowResult.getElapsedTimeSeconds() > fastResult.getElapsedTimeSeconds());

    request.withPreferences(pref -> pref.withScooter(scooter -> scooter.withReluctance(1)));
    State lowReluctanceResult = traverseStreetFromRental(
      testStreet,
      vehicleRentalEdge,
      link,
      rentalVertex,
      request.build()
    );

    request.withPreferences(pref -> pref.withScooter(scooter -> scooter.withReluctance(5)));

    State highReluctanceResult = traverseStreetFromRental(
      testStreet,
      vehicleRentalEdge,
      link,
      rentalVertex,
      request.build()
    );
    // Cost should be more when reluctance is higher but the time should be the same.
    assertTrue(highReluctanceResult.getWeight() > lowReluctanceResult.getWeight() + DELTA);

    assertEquals(
      highReluctanceResult.getElapsedTimeSeconds(),
      lowReluctanceResult.getElapsedTimeSeconds()
    );
  }

  @Test
  public void testWalkingBeforeScooter() {
    StreetEdge e1 = StreetModelForTest.streetEdgeBuilder(
      StreetModelForTest.V1,
      StreetModelForTest.V2,
      100.0,
      StreetTraversalPermission.ALL
    )
      .withCarSpeed(10.0f)
      .buildAndConnect();

    var request = StreetSearchRequest.of()
      .withPreferences(pref -> pref.withWalk(walk -> walk.withReluctance(1)))
      .withMode(StreetMode.SCOOTER_RENTAL);

    State s0 = new State(StreetModelForTest.V1, request.build());
    State result = e1.traverse(s0)[0];

    request.withPreferences(pref ->
      pref.withScooter(scooter -> scooter.withReluctance(5).withSpeed(8.5))
    );

    s0 = new State(StreetModelForTest.V1, request.build());
    var scooterReluctanceResult = e1.traverse(s0)[0];

    // Scooter preferences shouldn't affect walking when SCOOTER_RENTAL is used as mode
    assertEquals(TraverseMode.WALK, result.currentMode());
    assertEquals(result.getWeight(), scooterReluctanceResult.getWeight(), DELTA);
    assertEquals(result.getElapsedTimeSeconds(), scooterReluctanceResult.getElapsedTimeSeconds());
  }

  @Test
  public void testScooterOptimizeTriangle() {
    // This test does not depend on the setup method - and can probably be simplified
    Coordinate c1 = new Coordinate(-122.575033, 45.456773);
    Coordinate c2 = new Coordinate(-122.576668, 45.451426);

    var formFactor = RentalFormFactor.SCOOTER;

    var rentalVertex = StreetModelForTest.rentalVertex(formFactor);
    var vehicleRentalEdge = VehicleRentalEdge.createVehicleRentalEdge(rentalVertex, formFactor);

    StreetVertex v1 = StreetModelForTest.intersectionVertex("v1", c1.x, c1.y);
    StreetVertex v2 = StreetModelForTest.intersectionVertex("v2", c2.x, c2.y);

    var link = StreetVehicleRentalLink.createStreetVehicleRentalLink(rentalVertex, v1);

    GeometryFactory factory = new GeometryFactory();
    LineString geometry = factory.createLineString(new Coordinate[] { c1, c2 });

    double length = 650.0;

    StreetEdge testStreet = new StreetEdgeBuilder<>()
      .withFromVertex(v1)
      .withToVertex(v2)
      .withGeometry(geometry)
      .withName("Test Lane")
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.ALL)
      // a safe street
      .withBicycleSafetyFactor(0.74f)
      .buildAndConnect();

    Coordinate[] profile = new Coordinate[] {
      new Coordinate(0, 0), // slope = 0.1
      new Coordinate(length / 2, length / 20.0),
      new Coordinate(length, 0), // slope = -0.1
    };
    PackedCoordinateSequence elev = new PackedCoordinateSequence.Double(profile);
    StreetElevationExtensionBuilder.of(testStreet)
      .withElevationProfile(elev)
      .withComputed(false)
      .build()
      .ifPresent(testStreet::setElevationExtension);

    SlopeCosts costs = ElevationUtils.getSlopeCosts(elev, true);
    double trueLength = costs.lengthMultiplier * length;
    double slopeWorkLength = testStreet.getEffectiveBikeDistanceForWorkCost();
    double slopeSpeedLength = testStreet.getEffectiveBikeDistance();

    var request = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL);

    request.withPreferences(pref ->
      pref
        .withScooter(scooter ->
          scooter
            .withSpeed(SPEED)
            .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
            .withOptimizeTriangle(it -> it.withTime(1))
            .withReluctance(1)
        )
        .withWalk(walk -> walk.withReluctance(1))
        .withCar(car -> car.withReluctance(1))
    );

    var rentedState = vehicleRentalEdge.traverse(new State(rentalVertex, request.build()));
    var startState = link.traverse(rentedState[0])[0];

    State result = testStreet.traverse(startState)[0];
    double expectedTimeWeight = slopeSpeedLength / SPEED;
    assertEquals(TraverseMode.SCOOTER, result.currentMode());
    assertEquals(expectedTimeWeight, result.getWeight() - startState.getWeight(), DELTA);

    request.withPreferences(p ->
      p.withScooter(scooter -> scooter.withOptimizeTriangle(it -> it.withSlope(1)))
    );
    rentedState = vehicleRentalEdge.traverse(new State(rentalVertex, request.build()));
    startState = link.traverse(rentedState[0])[0];

    result = testStreet.traverse(startState)[0];
    double slopeWeight = result.getWeight();
    double expectedSlopeWeight = slopeWorkLength / SPEED;
    assertEquals(expectedSlopeWeight, slopeWeight - startState.getWeight(), DELTA);
    assertTrue((length * 1.5) / SPEED < slopeWeight);
    assertTrue((length * 1.5 * 10) / SPEED > slopeWeight);

    request.withPreferences(p ->
      p.withScooter(scooter -> scooter.withOptimizeTriangle(it -> it.withSafety(1)))
    );
    rentedState = vehicleRentalEdge.traverse(new State(rentalVertex, request.build()));
    startState = link.traverse(rentedState[0])[0];

    result = testStreet.traverse(startState)[0];
    double slopeSafety = costs.slopeSafetyCost;
    double safetyWeight = result.getWeight();
    double expectedSafetyWeight = (trueLength * 0.74 + slopeSafety) / SPEED;
    assertEquals(expectedSafetyWeight, safetyWeight - startState.getWeight(), DELTA);
  }

  private State traverseStreetFromRental(
    StreetEdge streetEdge,
    VehicleRentalEdge rentalEdge,
    StreetVehicleRentalLink link,
    VehicleRentalPlaceVertex rentalVertex,
    StreetSearchRequest request
  ) {
    var rentedState = rentalEdge.traverse(new State(rentalVertex, request));
    var startState = link.traverse(rentedState[0])[0];
    return streetEdge.traverse(startState)[0];
  }
}
