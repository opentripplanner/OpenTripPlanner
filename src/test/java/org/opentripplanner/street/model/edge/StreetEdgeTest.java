package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdgeBuilder;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.util.SlopeCosts;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.LabelledIntersectionVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestBuilder;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateData;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class StreetEdgeTest {

  private static final double DELTA = 0.00001;
  private static final double SPEED = 6.0;

  private IntersectionVertex v0, v1, v2;
  private StreetSearchRequest proto;

  @BeforeEach
  public void before() {
    v0 = intersectionVertex("maple_0th", 0.0, 0.0); // label, X, Y
    v1 = intersectionVertex("maple_1st", 2.0, 2.0);
    v2 = intersectionVertex("maple_2nd", 1.0, 2.0);

    this.proto =
      StreetSearchRequest
        .of()
        .withPreferences(references ->
          references
            .withStreet(s -> s.withTurnReluctance(1.0))
            .withWalk(it -> it.withSpeed(1.0).withReluctance(1.0).withStairsReluctance(1.0))
            .withBike(it ->
              it.withSpeed(5.0f).withReluctance(1.0).withWalking(w -> w.withSpeed(0.8))
            )
            .withCar(c -> c.withSpeed(15.0f).withReluctance(1.0))
        )
        .build();
  }

  @Test
  public void testInAndOutAngles() {
    // An edge heading straight West
    StreetEdge e1 = streetEdge(v1, v2, 1.0, StreetTraversalPermission.ALL);

    // Edge has same first and last angle.
    assertEquals(90, e1.getInAngle());
    assertEquals(90, e1.getOutAngle());

    // 2 new ones
    StreetVertex u = intersectionVertex("test1", 2.0, 1.0);
    StreetVertex v = intersectionVertex("test2", 2.0, 2.0);

    // Second edge, heading straight North
    StreetEdge e2 = streetEdge(u, v, 1.0, StreetTraversalPermission.ALL);

    // 180 degrees could be expressed as 180 or -180. Our implementation happens to use -180.
    assertEquals(180, Math.abs(e2.getInAngle()));
    assertEquals(180, Math.abs(e2.getOutAngle()));
  }

  @Test
  public void testTraverseAsPedestrian() {
    StreetEdge e1 = streetEdgeBuilder(v1, v2, 100.0, StreetTraversalPermission.ALL)
      .withCarSpeed(10.0f)
      .buildAndConnect();

    StreetSearchRequest options = StreetSearchRequest
      .copyOf(proto)
      .withMode(StreetMode.WALK)
      .build();

    State s0 = new State(v1, options);
    State s1 = e1.traverse(s0)[0];

    // Should use the speed on the edge.
    double expectedWeight = e1.getDistanceMeters() / options.preferences().walk().speed();
    long expectedDuration = (long) Math.ceil(expectedWeight);
    assertEquals(expectedDuration, s1.getElapsedTimeSeconds(), 0.0);
    assertEquals(expectedWeight, s1.getWeight(), 0.0);
  }

  @Test
  public void testTraverseAsCar() {
    StreetEdge e1 = streetEdgeBuilder(v1, v2, 100.0, StreetTraversalPermission.ALL)
      .withCarSpeed(10.0f)
      .buildAndConnect();

    State s0 = new State(v1, StreetSearchRequest.copyOf(proto).withMode(StreetMode.CAR).build());
    State s1 = e1.traverse(s0)[0];

    // Should use the speed on the edge.
    double expectedWeight = e1.getDistanceMeters() / e1.getCarSpeed();
    long expectedDuration = (long) Math.ceil(expectedWeight);
    assertEquals(expectedDuration, s1.getElapsedTimeSeconds(), 0.0);
    assertEquals(expectedWeight, s1.getWeight(), 0.0);
  }

  @Test
  public void testTraverseFloatingScooter() {
    // This test does not depend on the setup method - and can probably be simplified
    Coordinate c1 = new Coordinate(-122.575033, 45.456773);
    Coordinate c2 = new Coordinate(-122.576668, 45.451426);

    var formFactor = RentalFormFactor.SCOOTER;
    var rentalVertex = createRentalVertex(formFactor);
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
    StreetEdge e1 = streetEdgeBuilder(v1, v2, 100.0, StreetTraversalPermission.ALL)
      .withCarSpeed(10.0f)
      .buildAndConnect();

    var request = StreetSearchRequest
      .copyOf(proto)
      .withPreferences(pref -> pref.withWalk(walk -> walk.withReluctance(1)))
      .withMode(StreetMode.SCOOTER_RENTAL);

    State s0 = new State(v1, request.build());
    State result = e1.traverse(s0)[0];

    request.withPreferences(pref ->
      pref.withScooter(scooter -> scooter.withReluctance(5).withSpeed(8.5))
    );

    s0 = new State(v1, request.build());
    var scooterReluctanceResult = e1.traverse(s0)[0];

    // Scooter preferences shouldn't affect walking when SCOOTER_RENTAL is used as mode
    assertEquals(TraverseMode.WALK, result.currentMode());
    assertEquals(result.getWeight(), scooterReluctanceResult.getWeight(), DELTA);
    assertEquals(result.getElapsedTimeSeconds(), scooterReluctanceResult.getElapsedTimeSeconds());
  }

  @Test
  public void testModeSetCanTraverse() {
    StreetEdge e = streetEdge(v1, v2, 1.0, StreetTraversalPermission.ALL);

    TraverseModeSet modes = TraverseModeSet.allModes();
    assertTrue(e.canTraverse(modes));

    modes = new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK);
    assertTrue(e.canTraverse(modes));

    e = streetEdge(v1, v2, 1.0, StreetTraversalPermission.CAR);
    assertFalse(e.canTraverse(modes));

    modes = new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK);
    assertTrue(e.canTraverse(modes));
  }

  /**
   * Test the traversal of two edges with different traverse modes, with a focus on cycling. This
   * test will fail unless the following four conditions are met: 1. Turn costs are computed based
   * on the back edge's traverse mode during reverse traversal. 2. Turn costs are computed such that
   * bike walking is taken into account correctly. 3. User-specified bike speeds are applied
   * correctly during turn cost computation. 4. Traffic light wait time is taken into account.
   */
  @Test
  public void testTraverseModeSwitchBike() {
    var vWithTrafficLight = new LabelledIntersectionVertex("maple_1st", 2.0, 2.0, false, true);
    StreetEdge e0 = streetEdge(v0, vWithTrafficLight, 50.0, StreetTraversalPermission.PEDESTRIAN);
    StreetEdge e1 = streetEdge(
      vWithTrafficLight,
      v2,
      18.4,
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
    );

    StreetSearchRequestBuilder forward = StreetSearchRequest.copyOf(proto);
    forward.withPreferences(p -> p.withBike(it -> it.withSpeed(3.0f)));

    State s0 = new State(v0, forward.withMode(StreetMode.BIKE).build());
    State s1 = e0.traverse(s0)[0];
    State s2 = e1.traverse(s1)[0];

    StreetSearchRequestBuilder reverse = StreetSearchRequest.copyOf(proto);
    reverse.withArriveBy(true);
    reverse.withPreferences(p -> p.withBike(it -> it.withSpeed(3.0f)));

    State s3 = new State(v2, reverse.withMode(StreetMode.BIKE).build());
    State s4 = e1.traverse(s3)[0];
    State s5 = e0.traverse(s4)[0];

    assertEquals(88, s2.getElapsedTimeSeconds());
    assertEquals(88, s5.getElapsedTimeSeconds());
  }

  /**
   * Test the traversal of two edges with different traverse modes, with a focus on walking. This
   * test will fail unless the following four conditions are met: 1. Turn costs are computed based
   * on the back edge's traverse mode during reverse traversal. 2. Turn costs are computed such that
   * bike walking is taken into account correctly. 3. Enabling bike mode on a routing request bases
   * the bike walking speed on the walking speed. 4. Traffic light wait time is taken into account.
   */
  @Test
  public void testTraverseModeSwitchWalk() {
    var vWithTrafficLight = new LabelledIntersectionVertex("maple_1st", 2.0, 2.0, false, true);
    StreetEdge e0 = streetEdge(
      v0,
      vWithTrafficLight,
      50.0,
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
    );
    StreetEdge e1 = streetEdge(vWithTrafficLight, v2, 18.4, StreetTraversalPermission.PEDESTRIAN);

    StreetSearchRequestBuilder forward = StreetSearchRequest.copyOf(proto);

    State s0 = new State(v0, forward.withMode(StreetMode.BIKE).build());
    State s1 = e0.traverse(s0)[0];
    State s2 = e1.traverse(s1)[0];

    StreetSearchRequestBuilder reverse = StreetSearchRequest.copyOf(proto);
    reverse.withArriveBy(true);

    State s3 = new State(v2, reverse.withMode(StreetMode.BIKE).build());
    State s4 = e1.traverse(s3)[0];
    State s5 = e0.traverse(s4)[0];

    assertEquals(57, s2.getElapsedTimeSeconds());
    assertEquals(57, s5.getElapsedTimeSeconds());
  }

  /**
   * Test the bike switching penalty feature, both its cost penalty and its separate time penalty.
   */
  @Test
  public void testBikeSwitch() {
    StreetEdge e0 = streetEdge(v0, v1, 0.0, StreetTraversalPermission.PEDESTRIAN);
    StreetEdge e1 = streetEdge(v1, v2, 0.0, StreetTraversalPermission.BICYCLE);
    StreetEdge e2 = streetEdge(v2, v0, 0.0, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

    StreetSearchRequestBuilder noPenalty = StreetSearchRequest.copyOf(proto);
    noPenalty.withPreferences(p ->
      p.withBike(it -> it.withWalking(w -> w.withHopTime(0).withHopCost(0)))
    );

    State s0 = new State(v0, noPenalty.withMode(StreetMode.BIKE).build());
    State s1 = e0.traverse(s0)[0];
    State s2 = e1.traverse(s1)[0];
    State s3 = e2.traverse(s2)[0];

    StreetSearchRequestBuilder withPenalty = StreetSearchRequest.copyOf(proto);
    withPenalty.withPreferences(p ->
      p.withBike(it -> it.withWalking(w -> w.withHopTime(42).withHopCost(23)))
    );

    State s4 = new State(v0, withPenalty.withMode(StreetMode.BIKE).build());
    State s5 = e0.traverse(s4)[0];
    State s6 = e1.traverse(s5)[0];
    State s7 = e2.traverse(s6)[0];

    assertNull(s0.getBackMode());
    assertEquals(TraverseMode.WALK, s1.getBackMode());
    assertEquals(TraverseMode.BICYCLE, s2.getBackMode());
    assertEquals(TraverseMode.BICYCLE, s3.getBackMode());

    assertNull(s4.getBackMode());
    assertEquals(TraverseMode.WALK, s5.getBackMode());
    assertEquals(TraverseMode.BICYCLE, s6.getBackMode());
    assertEquals(TraverseMode.BICYCLE, s7.getBackMode());

    assertEquals(0, s0.getElapsedTimeSeconds());
    assertEquals(0, s1.getElapsedTimeSeconds());
    assertEquals(0, s2.getElapsedTimeSeconds());
    assertEquals(0, s3.getElapsedTimeSeconds());

    assertEquals(0.0, s0.getWeight(), 0.0);
    assertEquals(0.0, s1.getWeight(), 0.0);
    assertEquals(0.0, s2.getWeight(), 0.0);
    assertEquals(0.0, s3.getWeight(), 0.0);

    assertEquals(0.0, s4.getWeight(), 0.0);
    assertEquals(0.0, s5.getWeight(), 0.0);
    assertEquals(23.0, s6.getWeight(), 0.0);
    assertEquals(23.0, s7.getWeight(), 0.0);

    assertEquals(0, s4.getElapsedTimeSeconds());
    assertEquals(0, s5.getElapsedTimeSeconds());
    assertEquals(42, s6.getElapsedTimeSeconds());
    assertEquals(42, s7.getElapsedTimeSeconds());
  }

  @Test
  public void testTurnRestriction() {
    StreetEdge e0 = streetEdge(v0, v1, 50.0, StreetTraversalPermission.ALL);
    StreetEdge e1 = streetEdge(v1, v2, 18.4, StreetTraversalPermission.ALL);
    StreetSearchRequestBuilder streetSearchRequestBuilder = StreetSearchRequest.copyOf(proto);
    streetSearchRequestBuilder.withArriveBy(true);
    StreetSearchRequest request = streetSearchRequestBuilder.withMode(StreetMode.WALK).build();
    State state = new State(v2, Instant.EPOCH, StateData.getBaseCaseStateData(request), request);

    e1.addTurnRestriction(new TurnRestriction(e1, e0, null, TraverseModeSet.allModes(), null));

    assertNotNull(e0.traverse(e1.traverse(state)[0])[0]);
  }

  @Test
  public void testElevationProfile() {
    var elevationProfile = new PackedCoordinateSequence.Double(
      new double[] { 0, 10, 50, 12 },
      2,
      0
    );
    var edge = streetEdge(v0, v1, 50.0, StreetTraversalPermission.ALL);
    StreetElevationExtensionBuilder
      .of(edge)
      .withElevationProfile(elevationProfile)
      .withComputed(false)
      .build()
      .ifPresent(edge::setElevationExtension);

    assertArrayEquals(
      elevationProfile.toCoordinateArray(),
      edge.getElevationProfile().toCoordinateArray()
    );
  }

  @Test
  public void testBikeOptimizeTriangle() {
    // This test does not depend on the setup method - and can probably be simplified

    Coordinate c1 = new Coordinate(-122.575033, 45.456773);
    Coordinate c2 = new Coordinate(-122.576668, 45.451426);

    StreetVertex v1 = StreetModelForTest.intersectionVertex("v1", c1.x, c1.y);
    StreetVertex v2 = StreetModelForTest.intersectionVertex("v2", c2.x, c2.y);

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
      .withBack(false)
      // a safe street
      .withBicycleSafetyFactor(0.74f)
      .buildAndConnect();

    Coordinate[] profile = new Coordinate[] {
      new Coordinate(0, 0), // slope = 0.1
      new Coordinate(length / 2, length / 20.0),
      new Coordinate(length, 0), // slope = -0.1
    };
    PackedCoordinateSequence elev = new PackedCoordinateSequence.Double(profile);
    StreetElevationExtensionBuilder
      .of(testStreet)
      .withElevationProfile(elev)
      .withComputed(false)
      .build()
      .ifPresent(testStreet::setElevationExtension);

    SlopeCosts costs = ElevationUtils.getSlopeCosts(elev, true);
    double trueLength = costs.lengthMultiplier * length;
    double slopeWorkLength = testStreet.getEffectiveBikeDistanceForWorkCost();
    double slopeSpeedLength = testStreet.getEffectiveBikeDistance();

    var request = StreetSearchRequest.of().withMode(StreetMode.BIKE);

    request.withPreferences(pref ->
      pref
        .withBike(bike ->
          bike
            .withSpeed(SPEED)
            .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
            .withOptimizeTriangle(it -> it.withTime(1))
            .withReluctance(1)
        )
        .withWalk(walk -> walk.withReluctance(1))
        .withCar(car -> car.withReluctance(1))
    );

    State startState = new State(v1, request.build());
    State result = testStreet.traverse(startState)[0];
    double timeWeight = result.getWeight();
    double expectedTimeWeight = slopeSpeedLength / SPEED;
    assertEquals(expectedTimeWeight, result.getWeight(), DELTA);

    request.withPreferences(p ->
      p.withBike(bike -> bike.withOptimizeTriangle(it -> it.withSlope(1)))
    );
    startState = new State(v1, request.build());
    result = testStreet.traverse(startState)[0];
    double slopeWeight = result.getWeight();
    double expectedSlopeWeight = slopeWorkLength / SPEED;
    assertEquals(expectedSlopeWeight, slopeWeight, DELTA);
    assertTrue(length * 1.5 / SPEED < slopeWeight);
    assertTrue(length * 1.5 * 10 / SPEED > slopeWeight);

    request.withPreferences(p ->
      p.withBike(bike -> bike.withOptimizeTriangle(it -> it.withSafety(1)))
    );
    startState = new State(v1, request.build());
    result = testStreet.traverse(startState)[0];
    double slopeSafety = costs.slopeSafetyCost;
    double safetyWeight = result.getWeight();
    double expectedSafetyWeight = (trueLength * 0.74 + slopeSafety) / SPEED;
    assertEquals(expectedSafetyWeight, safetyWeight, DELTA);

    request.withPreferences(p ->
      p.withBike(bike -> bike.withOptimizeTriangle(it -> it.withTime(1).withSlope(1).withSafety(1)))
    );
    startState = new State(v1, request.build());
    result = testStreet.traverse(startState)[0];
    double expectedWeight = timeWeight * 0.33 + slopeWeight * 0.33 + safetyWeight * 0.34;
    assertEquals(expectedWeight, result.getWeight(), DELTA);
  }

  @Test
  public void testScooterOptimizeTriangle() {
    // This test does not depend on the setup method - and can probably be simplified
    Coordinate c1 = new Coordinate(-122.575033, 45.456773);
    Coordinate c2 = new Coordinate(-122.576668, 45.451426);

    var formFactor = RentalFormFactor.SCOOTER;

    var rentalVertex = createRentalVertex(formFactor);
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
    StreetElevationExtensionBuilder
      .of(testStreet)
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
    assertTrue(length * 1.5 / SPEED < slopeWeight);
    assertTrue(length * 1.5 * 10 / SPEED > slopeWeight);

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

  private VehicleRentalPlaceVertex createRentalVertex(RentalFormFactor formFactor) {
    VehicleRentalVehicle rentalVehicle = new VehicleRentalVehicle();

    var network = "1";
    rentalVehicle.latitude = -122.575133;
    rentalVehicle.longitude = 45.456773;
    rentalVehicle.id = new FeedScopedId(network, "123");
    rentalVehicle.vehicleType =
      new RentalVehicleType(
        new FeedScopedId(network, "type"),
        "type",
        formFactor,
        RentalVehicleType.PropulsionType.ELECTRIC,
        100000d
      );

    return new VehicleRentalPlaceVertex(rentalVehicle);
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
