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

    public static final double powerReductionFactor = 0.8; // used to make it easier to reason about specific power inputs
    public static final double allowableSpeedDelta = 0.1;

    public void testMicromobilityTravelTimeAtZeroSlope () {
        assertEquals(
            9.44,
            StreetEdge.calculateMicromobilitySpeed(
                250 / powerReductionFactor,
                105,
                Math.atan(0),
                0.005,
                ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                Double.NEGATIVE_INFINITY, // an obscene number to make sure min speed bounding is turned off
                Double.POSITIVE_INFINITY // an obscene number to make sure max speed bounding is turned off
            ),
            allowableSpeedDelta
        );
    }

    public void testMicromobilityTravelTimeWithIncline () {
        assertEquals(
            3.14,
            StreetEdge.calculateMicromobilitySpeed(
                250 / powerReductionFactor,
                105,
                Math.atan(0.07),
                0.005,
                ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                Double.NEGATIVE_INFINITY, // an obscene number to make sure min speed bounding is turned off
                Double.POSITIVE_INFINITY // an obscene number to make sure max speed bounding is turned off
            ),
            allowableSpeedDelta
        );
    }

    public void testMicromobilityTravelTimeWithDecline () {
        assertEquals(
            16.3,
            StreetEdge.calculateMicromobilitySpeed(
                250 / powerReductionFactor,
                105,
                Math.atan(-0.05),
                0.005,
                ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                Double.NEGATIVE_INFINITY, // an obscene number to make sure min speed bounding is turned off
                Double.POSITIVE_INFINITY // an obscene number to make sure max speed bounding is turned off
            ),
            allowableSpeedDelta
        );
    }

    public void testMicromobilityTravelTimeWithInclineAndMinSpeed () {
        assertEquals(
            0.8,
            StreetEdge.calculateMicromobilitySpeed(
                100 / powerReductionFactor,
                105,
                Math.atan(0.2),
                0.005,
                ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                0.8, // minimum speed in m/s
                Double.POSITIVE_INFINITY // an obscene number to make sure max speed bounding is turned off
            ),
            allowableSpeedDelta
        );
    }

    public void testMicromobilityTravelTimeWithDeclineAndMaxSpeed () {
        assertEquals(
            12.5,
            StreetEdge.calculateMicromobilitySpeed(
                250 / powerReductionFactor,
                105,
                Math.atan(-0.2),
                0.005,
                ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                Double.NEGATIVE_INFINITY, // an obscene number to make sure min speed bounding is turned off
                12.5 // maximum speed in m/s
            ),
            allowableSpeedDelta
        );
    }
}
