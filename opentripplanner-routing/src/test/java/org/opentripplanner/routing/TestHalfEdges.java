package org.opentripplanner.routing;

import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.location.StreetLocationFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class TestHalfEdges extends TestCase {

    Graph graph;

    private Edge leftUp;

    private Edge rightUp;

    private Edge top;

    private Edge bottom;

    private Edge leftDown;

    private Edge rightDown;

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
        Vertex tr = graph.addVertex("tr", -73, 41);
        Vertex bl = graph.addVertex("bl", -74, 40);
        Vertex br = graph.addVertex("br", -73, 40);

        double td = GtfsLibrary.distance(tl.getCoordinate().y, tl.getCoordinate().x, tr
                .getCoordinate().y, tr.getCoordinate().x);
        Street topStreet = new Street(td);
        topStreet.setGeometry(createGeometry(tl, tr));
        top = graph.addEdge(tl, tr, topStreet);

        double bd = GtfsLibrary.distance(bl.getCoordinate().y, bl.getCoordinate().x, br
                .getCoordinate().y, br.getCoordinate().x);
        Street bottomStreet = new Street(bd);
        topStreet.setGeometry(createGeometry(bl, br));
        bottom = graph.addEdge(bl, br, bottomStreet);

        double d = GtfsLibrary.distance(bl.getCoordinate().y, bl.getCoordinate().x, tl
                .getCoordinate().y, tl.getCoordinate().x);

        Street leftDownStreet = new Street(d);
        leftDownStreet.setGeometry(createGeometry(tl, bl));
        leftDown = graph.addEdge(tl, bl, leftDownStreet);

        Street leftUpStreet = new Street(d);
        leftUpStreet.setGeometry(createGeometry(bl, tl));
        leftUp = graph.addEdge(bl, tl, leftUpStreet);

        Street rightDownStreet = new Street(d);
        rightDownStreet.setGeometry(createGeometry(tr, br));
        rightDown = graph.addEdge(tr, br, rightDownStreet);

        Street rightUpStreet = new Street(d);
        rightUpStreet.setGeometry(createGeometry(tr, br));
        rightUp = graph.addEdge(br, tr, rightUpStreet);
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

        System.out.println("path: " + path.vertices);

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
