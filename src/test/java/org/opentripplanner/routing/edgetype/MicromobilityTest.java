package org.opentripplanner.routing.edgetype;

import junit.framework.TestCase;
import org.junit.Test;
import org.opentripplanner.routing.util.ElevationUtils;

public class MicromobilityTest extends TestCase {
    @Test public void canCalculateDragResistiveComponent () {
        double allowableDelta = 0.0001;

        // elevation at sea level
        assertEquals(0.4553, ElevationUtils.getDragResistiveForceComponent(0), allowableDelta);

        // elevation at 2,000 meters
        assertEquals(0.3801, ElevationUtils.getDragResistiveForceComponent(2000), allowableDelta);
    }

    public static final double travelDistance = 1000;  // 1km
    public static final double kphToMps = 1000.0 / 3600;
    public static final double allowableTravelTimeDelta = 0.1;

    @Test public void canCalculateMicromobilityTravelTimeAtZeroSlope () {
        double expectedSpeedInKph = 34.092;
        assertEquals(
            travelDistance / (expectedSpeedInKph * kphToMps),
            StreetEdge.calculateMicromobilityTravelTime(
                250 / 0.9,
                105,
                Math.atan(0),
                0.005,
                ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                Double.NEGATIVE_INFINITY, // an obscene number to make sure bounding is turned off
                Double.POSITIVE_INFINITY, // an obscene number to make sure bounding is turned off
                travelDistance
            ),
            allowableTravelTimeDelta
        );
    }
}
