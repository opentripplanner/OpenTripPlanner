package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.util.SlopeCosts;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.util.NonLocalizedString;

public class TriangleTest {

    float DELTA = 0.001f;

    @Test
    public void testPreciseValues() {
        var req = new RoutingRequest();
        req.setTriangleNormalized(0.5,0.4,0.1);
        assertEquals(req.bikeTriangleSafetyFactor, 0.5, DELTA);
        assertEquals(req.bikeTriangleSlopeFactor, 0.4, DELTA);
        assertEquals(req.bikeTriangleTimeFactor, 0.1, DELTA);
    }

    @Test
    public void testNormalizationGreaterThan1() {
        var req = new RoutingRequest();
        req.setTriangleNormalized(1,1,1);
        assertEquals(req.bikeTriangleSafetyFactor, 0.333, DELTA);
        assertEquals(req.bikeTriangleSlopeFactor, 0.333, DELTA);
        assertEquals(req.bikeTriangleTimeFactor, 0.333, DELTA);

        req.setTriangleNormalized(10,20,30);
        assertEquals(req.bikeTriangleSafetyFactor, 0.166, DELTA);
        assertEquals(req.bikeTriangleSlopeFactor, 0.333, DELTA);
        assertEquals(req.bikeTriangleTimeFactor, 0.5, DELTA);
    }

    @Test
    public void testNormalizationLessThan1() {
        var req = new RoutingRequest();
        req.setTriangleNormalized(0.1,0.1,0.1);
        assertEquals(req.bikeTriangleSafetyFactor, 0.333, DELTA);
        assertEquals(req.bikeTriangleSlopeFactor, 0.333, DELTA);
        assertEquals(req.bikeTriangleTimeFactor, 0.333, DELTA);
    }

    @Test
    public void testZero() {
        var req = new RoutingRequest();
        req.setTriangleNormalized(0,0,0.1);
        assertEquals(req.bikeTriangleSafetyFactor, 0, DELTA);
        assertEquals(req.bikeTriangleSlopeFactor, 0, DELTA);
        assertEquals(req.bikeTriangleTimeFactor, 1, DELTA);

        req.setTriangleNormalized(0,0,0);
        assertEquals(req.bikeTriangleSafetyFactor, 0.333, DELTA);
        assertEquals(req.bikeTriangleSlopeFactor, 0.333, DELTA);
        assertEquals(req.bikeTriangleTimeFactor, 0.333, DELTA);
    }

    @Test
    public void testLessThanZero() {
        var req = new RoutingRequest();
        req.setTriangleNormalized(-1,-1,0.1);
        assertEquals(req.bikeTriangleSafetyFactor, 0, DELTA);
        assertEquals(req.bikeTriangleSlopeFactor, 0, DELTA);
        assertEquals(req.bikeTriangleTimeFactor, 1, DELTA);
    }

    @Test
    public void testTriangle() {
        Coordinate c1 = new Coordinate(-122.575033, 45.456773);
        Coordinate c2 = new Coordinate(-122.576668, 45.451426);

        StreetVertex v1 = new IntersectionVertex(null, "v1", c1.x, c1.y, (NonLocalizedString)null);
        StreetVertex v2 = new IntersectionVertex(null, "v2", c2.x, c2.y, (NonLocalizedString)null);

        GeometryFactory factory = new GeometryFactory();
        LineString geometry = factory.createLineString(new Coordinate[] { c1, c2 });

        double length = 650.0;

        StreetWithElevationEdge testStreet = new StreetWithElevationEdge(v1, v2, geometry, "Test Lane", length,
                StreetTraversalPermission.ALL, false);
        testStreet.setBicycleSafetyFactor(0.74f); // a safe street

        Coordinate[] profile = new Coordinate[] { 
                new Coordinate(0, 0), // slope = 0.1
                new Coordinate(length / 2, length / 20.0), 
                new Coordinate(length, 0) // slope = -0.1
        };
        PackedCoordinateSequence elev = new PackedCoordinateSequence.Double(profile);
        testStreet.setElevationProfile(elev, false);

        SlopeCosts costs = ElevationUtils.getSlopeCosts(elev, true);
        double trueLength = costs.lengthMultiplier * length;
        double slopeWorkLength = testStreet.getEffectiveBikeWorkCost();
        double slopeSpeedLength = testStreet.getEffectiveBikeDistance();

        RoutingRequest options = new RoutingRequest(TraverseMode.BICYCLE);
        options.bicycleOptimizeType = BicycleOptimizeType.TRIANGLE;
        options.bikeSpeed = 6.0;
        options.setNonTransitReluctance(1);

        options.setTriangleNormalized(0, 0, 1);
        State startState = new State(v1, options);
        State result = testStreet.traverse(startState);
        double timeWeight = result.getWeight();
        double expectedTimeWeight = slopeSpeedLength / options.getSpeed(TraverseMode.BICYCLE, false);
        assertTrue(Math.abs(expectedTimeWeight - timeWeight) < 0.00001);

        options.setTriangleNormalized(0, 1, 0);
        startState = new State(v1, options);
        result = testStreet.traverse(startState);
        double slopeWeight = result.getWeight();
        double expectedSlopeWeight = slopeWorkLength / options.getSpeed(TraverseMode.BICYCLE, false);
        assertTrue(Math.abs(expectedSlopeWeight - slopeWeight) < 0.00001);
        assertTrue(length * 1.5 / options.getSpeed(TraverseMode.BICYCLE, false) < slopeWeight);
        assertTrue(length * 1.5 * 10 / options.getSpeed(TraverseMode.BICYCLE, false) > slopeWeight);

        options.setTriangleNormalized(1, 0, 0);
        startState = new State(v1, options);
        result = testStreet.traverse(startState);
        double safetyWeight = result.getWeight();
        double slopeSafety = costs.slopeSafetyCost;
        double expectedSafetyWeight = (trueLength * 0.74 + slopeSafety) / options.getSpeed(TraverseMode.BICYCLE,
                false
        );
        assertTrue(Math.abs(expectedSafetyWeight - safetyWeight) < 0.00001);

        final double ONE_THIRD = 1/3.0;
        options.setTriangleNormalized(ONE_THIRD, ONE_THIRD, ONE_THIRD);
        startState = new State(v1, options);
        result = testStreet.traverse(startState);
        double averageWeight = result.getWeight();
        assertTrue(Math.abs(safetyWeight * ONE_THIRD + slopeWeight * ONE_THIRD + timeWeight * ONE_THIRD - averageWeight) < 0.00000001);

    }
}