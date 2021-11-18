package org.opentripplanner.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.util.model.EncodedPolylineBean;

public class PolylineEncoderTest {

    @Test
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

    @Test
    public void testPolygon() {
        var polygon = GeometryUtils.getGeometryFactory()
                .createPolygon(new Coordinate[]{
                        new Coordinate(0, 0),
                        new Coordinate(1, 1),
                        new Coordinate(2, 2),
                        new Coordinate(0, 0)
                });
        var polyline = PolylineEncoder.createEncodings(polygon);

        assertEquals("??_ibE_ibE_ibE_ibE~reK~reK", polyline.getPoints());
    }

    @Test
    public void testPoint() {
        var point = GeometryUtils.getGeometryFactory().createPoint(new Coordinate(100, 100));
        var polyline = PolylineEncoder.createEncodings(point);

        assertEquals("_gjaR_gjaR", polyline.getPoints());
    }
}
