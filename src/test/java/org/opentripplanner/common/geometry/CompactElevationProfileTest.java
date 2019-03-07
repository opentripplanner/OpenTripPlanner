package org.opentripplanner.common.geometry;

import junit.framework.TestCase;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

public class CompactElevationProfileTest extends TestCase {

    public final void testEncodingDecoding() {

        runOneTest(null);
        runOneTest(new Coordinate[] { new Coordinate(0.0, 0.0) });
        runOneTest(new Coordinate[] { new Coordinate(0.0, 1.0) });
        runOneTest(new Coordinate[] { new Coordinate(0.0, 10000.0) }); // More than Mt Everest
                                                                       // elevation
        runOneTest(new Coordinate[] { new Coordinate(100000.0, 0.0) }); // A long street segment
                                                                        // length
        runOneTest(new Coordinate[] { new Coordinate(0.0, 10.0), new Coordinate(8.0, -10.0),
                new Coordinate(120.0, 20.0), new Coordinate(150.0, 0.0) });
        runOneTest(new Coordinate[] { new Coordinate(0.0, 0.12345678), new Coordinate(1.1111111111, -0.987654321),
                new Coordinate(2.222222222222, 0.0000123), new Coordinate(3.33333333333, 6789.987654321) });
    }

    private void runOneTest(Coordinate[] c) {
        CoordinateSequence elev1 = c == null ? null : new PackedCoordinateSequence.Double(c);
        byte[] packed = CompactElevationProfile.compactElevationProfile(elev1);
        CoordinateSequence elev2 = CompactElevationProfile.uncompactElevationProfile(packed);
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
