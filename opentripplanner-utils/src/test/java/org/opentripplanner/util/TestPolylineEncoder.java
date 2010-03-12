package org.opentripplanner.util;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.util.model.EncodedPolylineBean;

import com.vividsolutions.jts.geom.Coordinate;

import junit.framework.TestCase;

public class TestPolylineEncoder extends TestCase {

    public void testCreateEncodingsIterableOfCoordinate() {
        // test taken from example usage
        List<Coordinate> points = new ArrayList<Coordinate>();
        points.add(new Coordinate(-73.85062, 40.903125, Double.NaN));
        points.add(new Coordinate(-73.85136, 40.902261, Double.NaN));
        points.add(new Coordinate(-73.85151, 40.902066, Double.NaN));
        EncodedPolylineBean eplb = PolylineEncoder.createEncodings(points);
        assertEquals("o{sxFl}vaMjDpCf@\\", eplb.getPoints());
        assertEquals(3, eplb.getLength());
        assertNull(eplb.getLevels());
    }
}
