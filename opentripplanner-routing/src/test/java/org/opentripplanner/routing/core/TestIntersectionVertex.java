package org.opentripplanner.routing.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import junit.framework.TestCase;

public class TestIntersectionVertex extends TestCase {

    public void testIntersectionVertex() {

        Intersection intersection = new Intersection("Morx at Fleem", 0, 0);
        GeometryFactory gf = new GeometryFactory();

        Geometry geometry = gf.createLineString(new Coordinate[] {
                new Coordinate(-10, 0),
                new Coordinate(0, 0)
                });

        IntersectionVertex leftV = new IntersectionVertex(intersection, geometry, true);

        Geometry geometry2 = gf.createLineString(new Coordinate[] {
                new Coordinate(0, 0),
                new Coordinate(10, 0)
                });

        IntersectionVertex rightV = new IntersectionVertex(intersection, geometry2, false);

        assertEquals(180, Math.abs(leftV.angle - rightV.angle));

    }

}
