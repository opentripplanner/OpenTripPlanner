package org.opentripplanner.common.geometry;

import junit.framework.TestCase;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

public class CompactElevationProfileTest extends TestCase {

    public final void testEncodingDecoding() {
        CompactElevationProfile.setDistanceBetweenSamplesM(
                CompactElevationProfile.DEFAULT_DISTANCE_BETWEEN_SAMPLES_METERS);

        runOneTest(new Coordinate[] {
                new Coordinate(0.0, 0.0),
                new Coordinate(3.0, 0.0),
        },
        3);

        runOneTest(new Coordinate[] {
                new Coordinate(0.0, 0.0),
                new Coordinate(10.0, 0.0),
                new Coordinate(19.0, 0.0)
        },
        19);
    }

    private void runOneTest(Coordinate[] c, double length) {
        CoordinateSequence elev1 = c == null ? null : new PackedCoordinateSequence.Double(c);
        byte[] packed = CompactElevationProfile.compactElevationProfileWithRegularSamples(elev1);
        CoordinateSequence elev2 = CompactElevationProfile
                .uncompactElevationProfileWithRegularSamples(packed, length);
        if (elev1 == null) {
            // This is rather simple
            assertNull(elev2);
            return;
        }
        assertEquals(elev1.size(), elev2.size());
        for (int i = 0; i < elev1.size(); i++) {
            Coordinate c1 = elev1.getCoordinate(i);
            Coordinate c2 = elev2.getCoordinate(i);
            double dx = Math.abs(c1.x - c2.x);
            double dy = Math.abs(c1.y - c2.y);
            assertTrue("Too large arc length delta", dx <= 1e-2);
            assertTrue("Too large elevation delta", dy <= 1e-2);
        }
    }
}
