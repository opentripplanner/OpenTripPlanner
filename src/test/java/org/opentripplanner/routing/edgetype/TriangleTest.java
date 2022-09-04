package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.util.SlopeCosts;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.transit.model.basic.NonLocalizedString;

public class TriangleTest {

  float DELTA = 0.001f;

  @Test
  public void testPreciseValues() {
    var pref = new BikePreferences();
    pref.setTriangleNormalized(0.5, 0.4, 0.1);

    assertEquals(pref.triangleSafetyFactor(), 0.5, DELTA);
    assertEquals(pref.triangleSlopeFactor(), 0.4, DELTA);
    assertEquals(pref.triangleTimeFactor(), 0.1, DELTA);
  }

  @Test
  public void testNormalizationGreaterThan1() {
    var pref = new BikePreferences();
    pref.setTriangleNormalized(1, 1, 1);
    assertEquals(pref.triangleSafetyFactor(), 0.333, DELTA);
    assertEquals(pref.triangleSlopeFactor(), 0.333, DELTA);
    assertEquals(pref.triangleTimeFactor(), 0.333, DELTA);

    pref.setTriangleNormalized(10, 20, 30);
    assertEquals(pref.triangleSafetyFactor(), 0.166, DELTA);
    assertEquals(pref.triangleSlopeFactor(), 0.333, DELTA);
    assertEquals(pref.triangleTimeFactor(), 0.5, DELTA);
  }

  @Test
  public void testNormalizationLessThan1() {
    var pref = new BikePreferences();
    pref.setTriangleNormalized(0.1, 0.1, 0.1);
    assertEquals(pref.triangleSafetyFactor(), 0.333, DELTA);
    assertEquals(pref.triangleSlopeFactor(), 0.333, DELTA);
    assertEquals(pref.triangleTimeFactor(), 0.333, DELTA);
  }

  @Test
  public void testZero() {
    var pref = new BikePreferences();
    pref.setTriangleNormalized(0, 0, 0.1);
    assertEquals(pref.triangleSafetyFactor(), 0, DELTA);
    assertEquals(pref.triangleSlopeFactor(), 0, DELTA);
    assertEquals(pref.triangleTimeFactor(), 1, DELTA);

    pref.setTriangleNormalized(0, 0, 0);
    assertEquals(pref.triangleSafetyFactor(), 0.333, DELTA);
    assertEquals(pref.triangleSlopeFactor(), 0.333, DELTA);
    assertEquals(pref.triangleTimeFactor(), 0.333, DELTA);
  }

  @Test
  public void testLessThanZero() {
    var pref = new BikePreferences();
    pref.setTriangleNormalized(-1, -1, 0.1);
    assertEquals(pref.triangleSafetyFactor(), 0, DELTA);
    assertEquals(pref.triangleSlopeFactor(), 0, DELTA);
    assertEquals(pref.triangleTimeFactor(), 1, DELTA);
  }

  @Test
  public void testTriangle() {
    Coordinate c1 = new Coordinate(-122.575033, 45.456773);
    Coordinate c2 = new Coordinate(-122.576668, 45.451426);

    StreetVertex v1 = new IntersectionVertex(null, "v1", c1.x, c1.y, (NonLocalizedString) null);
    StreetVertex v2 = new IntersectionVertex(null, "v2", c2.x, c2.y, (NonLocalizedString) null);

    GeometryFactory factory = new GeometryFactory();
    LineString geometry = factory.createLineString(new Coordinate[] { c1, c2 });

    double length = 650.0;

    StreetEdge testStreet = new StreetEdge(
      v1,
      v2,
      geometry,
      "Test Lane",
      length,
      StreetTraversalPermission.ALL,
      false
    );
    testStreet.setBicycleSafetyFactor(0.74f); // a safe street

    Coordinate[] profile = new Coordinate[] {
      new Coordinate(0, 0), // slope = 0.1
      new Coordinate(length / 2, length / 20.0),
      new Coordinate(length, 0), // slope = -0.1
    };
    PackedCoordinateSequence elev = new PackedCoordinateSequence.Double(profile);
    StreetElevationExtension.addToEdge(testStreet, elev, false);

    SlopeCosts costs = ElevationUtils.getSlopeCosts(elev, true);
    double trueLength = costs.lengthMultiplier * length;
    double slopeWorkLength = testStreet.getEffectiveBikeDistanceForWorkCost();
    double slopeSpeedLength = testStreet.getEffectiveBikeDistance();

    var request = new RouteRequest(TraverseMode.BICYCLE);

    var bikePreferences = request.preferences().bike();
    bikePreferences.setOptimizeType(BicycleOptimizeType.TRIANGLE);
    bikePreferences.setSpeed(6.0);
    request.preferences().setNonTransitReluctance(1);

    bikePreferences.setTriangleNormalized(0, 0, 1);
    State startState = new State(v1, request, null);
    State result = testStreet.traverse(startState);
    double timeWeight = result.getWeight();
    double expectedTimeWeight =
      slopeSpeedLength / request.preferences().getSpeed(TraverseMode.BICYCLE, false);
    assertTrue(Math.abs(expectedTimeWeight - timeWeight) < 0.00001);

    bikePreferences.setTriangleNormalized(0, 1, 0);
    startState = new State(v1, request, null);
    result = testStreet.traverse(startState);
    double slopeWeight = result.getWeight();
    double expectedSlopeWeight =
      slopeWorkLength / request.preferences().getSpeed(TraverseMode.BICYCLE, false);
    assertTrue(Math.abs(expectedSlopeWeight - slopeWeight) < 0.00001);
    assertTrue(
      length * 1.5 / request.preferences().getSpeed(TraverseMode.BICYCLE, false) < slopeWeight
    );
    assertTrue(
      length * 1.5 * 10 / request.preferences().getSpeed(TraverseMode.BICYCLE, false) > slopeWeight
    );

    bikePreferences.setTriangleNormalized(1, 0, 0);
    startState = new State(v1, request, null);
    result = testStreet.traverse(startState);
    double safetyWeight = result.getWeight();
    double slopeSafety = costs.slopeSafetyCost;
    double expectedSafetyWeight =
      (trueLength * 0.74 + slopeSafety) /
      request.preferences().getSpeed(TraverseMode.BICYCLE, false);
    assertTrue(Math.abs(expectedSafetyWeight - safetyWeight) < 0.00001);

    final double ONE_THIRD = 1 / 3.0;
    bikePreferences.setTriangleNormalized(ONE_THIRD, ONE_THIRD, ONE_THIRD);
    startState = new State(v1, request, null);
    result = testStreet.traverse(startState);
    double averageWeight = result.getWeight();
    assertTrue(
      Math.abs(
        safetyWeight * ONE_THIRD + slopeWeight * ONE_THIRD + timeWeight * ONE_THIRD - averageWeight
      ) <
      0.00000001
    );
  }
}
