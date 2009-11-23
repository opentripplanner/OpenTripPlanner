package org.opentripplanner.routing;

import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.location.StreetLocationFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextypes.Intersection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class TestHalfEdges extends TestCase {

    Graph graph;

    private Street leftUp;

    private Street rightUp;

    private Street top;

    private Street bottom;

    private Street leftDown;

    private Street rightDown;

    public LineString createGeometry(Vertex a, Vertex b) {

        GeometryFactory factory = new GeometryFactory();
        Coordinate[] cs = new Coordinate[2];
        cs[0] = a.getCoordinate();
        cs[1] = b.getCoordinate();
        return factory.createLineString(cs);
    }

    public void setUp() {
        graph = new Graph();
        // a 1 degree x 1 degree square
        Vertex tl = graph.addVertex("tl", -74, 41);
        tl.setType(Intersection.class);
        Vertex tr = graph.addVertex("tr", -73, 41);
        tr.setType(Intersection.class);
        Vertex bl = graph.addVertex("bl", -74, 40);
        bl.setType(Intersection.class);
        Vertex br = graph.addVertex("br", -73, 40);
        br.setType(Intersection.class);

        double td = DistanceLibrary.distance(tl.getCoordinate().y, tl.getCoordinate().x, tr
                .getCoordinate().y, tr.getCoordinate().x);
        top = new Street(tl, tr, td);
        top.setGeometry(createGeometry(tl, tr));
        graph.addEdge(top);

        double bd = DistanceLibrary.distance(bl.getCoordinate().y, bl.getCoordinate().x, br
                .getCoordinate().y, br.getCoordinate().x);
        bottom = new Street(bl, br, bd);
        bottom.setGeometry(createGeometry(bl, br));
        graph.addEdge(bottom);

        double d = DistanceLibrary.distance(bl.getCoordinate().y, bl.getCoordinate().x, tl
                .getCoordinate().y, tl.getCoordinate().x);

        leftDown = new Street(tl, bl, d);
        leftDown.setGeometry(createGeometry(tl, bl));
        graph.addEdge(leftDown);

        leftUp = new Street(bl, tl, d);
        leftUp.setGeometry(createGeometry(bl, tl));
        graph.addEdge(leftUp);

        rightDown = new Street(tr, br, d);
        rightDown.setGeometry(createGeometry(tr, br));
        graph.addEdge(rightDown);

        rightUp = new Street(br, tr, d);
        rightUp.setGeometry(createGeometry(tr, br));
        graph.addEdge(rightUp);
    }

    public void testHalfEdges() {
        // the shortest half-edge from the start vertex takes you down, but the shortest total path
        // is up and over

        StreetLocation start = new StreetLocation(leftUp, 0.4, false);
        StreetLocation end = new StreetLocation(rightUp, 0.8, true);

        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 1, 12, 34, 25);

        ShortestPathTree spt = AStar.getShortestPathTree(graph, start, end, new State(startTime
                .getTimeInMillis()), new TraverseOptions());

        GraphPath path = spt.getPath(end.vertex);
        assertNotNull(path);

        // the bottom-left point is not part of the shortest path
        for (SPTVertex v : path.vertices) {
            assertNotSame(v.mirror, graph.getVertex("bl"));
        }

    }

    public void testStreetLocationFinder() {
        StreetLocationFinder finder = new StreetLocationFinder(graph);

        StreetLocation start = finder.findLocation(new Coordinate(-74.0, 40.4), false);
        StreetLocation end = finder.findLocation(new Coordinate(-73.0, 40.8), true);

        if (start.street == leftUp) {
            assertTrue(start.location - 0.4 < 0.00001);
        } else {
            assertTrue(start.location - 0.6 < 0.00001);
            assertEquals(leftDown, start.street);
        }
        assertTrue(start.vertex.getDegreeOut() == 2);

        if (end.street == rightUp) {
            assertTrue(end.location - 0.2 < 0.00001);
        } else {
            assertTrue(end.location - 0.8 < 0.00001);
            assertEquals(rightDown, end.street);
        }
        assertTrue(end.vertex.getDegreeIn() == 2);
    }
}
