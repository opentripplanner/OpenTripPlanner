package org.opentripplanner.routing.core;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.GeometryFactory;

import junit.framework.TestCase;

public class TestTurns extends TestCase {

    public void testIntersectionVertex() {

        GeometryFactory gf = new GeometryFactory();

        LineString geometry = gf.createLineString(new Coordinate[] {
                new Coordinate(-0.10, 0),
                new Coordinate(0, 0)
                });

        IntersectionVertex v1 = new IntersectionVertex(null, "v1", -0.10, 0);
        IntersectionVertex v2 = new IntersectionVertex(null, "v2", 0, 0);
        
        StreetEdge leftEdge = new StreetEdge(v1, v2, geometry, "morx", 10.0, StreetTraversalPermission.ALL, true);

        LineString geometry2 = gf.createLineString(new Coordinate[] {
                new Coordinate(0, 0),
                new Coordinate(-0.10, 0)
                });

        StreetEdge rightEdge = new StreetEdge(v1, v2, geometry2, "fleem", 10.0, StreetTraversalPermission.ALL, false);

        assertEquals(180, Math.abs(leftEdge.getOutAngle() - rightEdge.getOutAngle()));

    }

}
