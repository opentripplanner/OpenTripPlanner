package org.opentripplanner.routing.edgetype;

import junit.framework.TestCase;
import org.opentripplanner.routing.util.ElevationUtils;

public class MicromobilityTest extends TestCase {

    public void testDragResistiveComponent () {
        double allowableDelta = 0.0001;

        // elevation at sea level
        assertEquals(0.4553, ElevationUtils.getDragResistiveForceComponent(0), allowableDelta);

        // elevation at 2,000 meters
        assertEquals(0.3801, ElevationUtils.getDragResistiveForceComponent(2000), allowableDelta);
    }

    public static final double travelDistance = 1000;  // 1km
    public static final double kphToMps = 1000.0 / 3600;
    public static final double allowableTravelTimeDelta = 1;

    public void testMicromobilityTravelTimeAtZeroSlope () {
        double expectedSpeedInKph = 34;
        assertEquals(
            travelDistance / (expectedSpeedInKph * kphToMps),
            StreetEdge.calculateMicromobilityTravelTime(
                250 / 0.9,
                105,
                Math.atan(0),
                0.005,
                ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                Double.NEGATIVE_INFINITY, // an obscene number to make sure min speed bounding is turned off
                Double.POSITIVE_INFINITY, // an obscene number to make sure max speed bounding is turned off
                travelDistance
            ),
            allowableTravelTimeDelta
        );
    }

    public void testMicromobilityTravelTimeWithIncline () {
        double expectedSpeedInKph = 11.3;
        assertEquals(
            travelDistance / (expectedSpeedInKph * kphToMps),
            StreetEdge.calculateMicromobilityTravelTime(
                250 / 0.9,
                105,
                Math.atan(0.07),
                0.005,
                ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                Double.NEGATIVE_INFINITY, // an obscene number to make sure min speed bounding is turned off
                Double.POSITIVE_INFINITY, // an obscene number to make sure max speed bounding is turned off
                travelDistance
            ),
            allowableTravelTimeDelta
        );
    }

    public void testMicromobilityTravelTimeWithDecline () {
        double expectedSpeedInKph = 59;
        assertEquals(
            travelDistance / (expectedSpeedInKph * kphToMps),
            StreetEdge.calculateMicromobilityTravelTime(
                250 / 0.9,
                105,
                Math.atan(-0.05),
                0.005,
                ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                Double.NEGATIVE_INFINITY, // an obscene number to make sure min speed bounding is turned off
                Double.POSITIVE_INFINITY, // an obscene number to make sure max speed bounding is turned off
                travelDistance
            ),
            allowableTravelTimeDelta
        );
    }

    public void testMicromobilityTravelTimeWithInclineAndMinSpeed () {
        double expectedSpeedInKph = 2.88;
        assertEquals(
            travelDistance / (expectedSpeedInKph * kphToMps),
            StreetEdge.calculateMicromobilityTravelTime(
                100 / 0.9,
                105,
                Math.atan(0.2),
                0.005,
                ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                0.8, // minimum speed in m/s
                Double.POSITIVE_INFINITY, // an obscene number to make sure max speed bounding is turned off
                travelDistance
            ),
            allowableTravelTimeDelta
        );
    }

    public void testMicromobilityTravelTimeWithDeclineAndMaxSpeed () {
        double expectedSpeedInKph = 45;
        assertEquals(
            travelDistance / (expectedSpeedInKph * kphToMps),
            StreetEdge.calculateMicromobilityTravelTime(
                250 / 0.9,
                105,
                Math.atan(-0.2),
                0.005,
                ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                Double.NEGATIVE_INFINITY, // an obscene number to make sure min speed bounding is turned off
                12.5, // maximum speed in m/s
                travelDistance
            ),
            allowableTravelTimeDelta
        );
    }
}
