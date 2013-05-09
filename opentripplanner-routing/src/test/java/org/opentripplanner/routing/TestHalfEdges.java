/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing;

import static org.opentripplanner.common.IterableLibrary.cast;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.loader.NetworkLinker;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.TestUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;

public class TestHalfEdges extends TestCase {

    Graph graph;

    private GenericAStar aStar = new GenericAStar();

    private PlainStreetEdge top, bottom, left, right, leftBack, rightBack;

    private IntersectionVertex br, tr, bl, tl;

    private TransitStop station1;

    private TransitStop station2;

    public LineString createGeometry(Vertex a, Vertex b) {
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] cs = new Coordinate[2];
        cs[0] = a.getCoordinate();
        cs[1] = b.getCoordinate();
        return factory.createLineString(cs);
    }

    public void setUp() {
        graph = new Graph();
        // a 0.1 degree x 0.1 degree square
        tl = new IntersectionVertex(graph, "tl", -74.01, 40.01);
        tr = new IntersectionVertex(graph, "tr", -74.0, 40.01);
        bl = new IntersectionVertex(graph, "bl", -74.01, 40.0);
        br = new IntersectionVertex(graph, "br", -74.00, 40.0);

        top = new PlainStreetEdge(tl, tr,
                GeometryUtils.makeLineString(-74.01, 40.01, -74.0, 40.01), "top", 1500,
                StreetTraversalPermission.ALL, false);
        bottom = new PlainStreetEdge(br, bl,
                GeometryUtils.makeLineString(-74.01, 40.0, -74.0, 40.0), "bottom", 1500,
                StreetTraversalPermission.ALL, false);
        left = new PlainStreetEdge(bl, tl,
                GeometryUtils.makeLineString(-74.01, 40.0, -74.01, 40.01), "left", 1500,
                StreetTraversalPermission.ALL, false);
        right = new PlainStreetEdge(br, tr,
                GeometryUtils.makeLineString(-74.0, 40.0, -74.0, 40.01), "right", 1500,
                StreetTraversalPermission.PEDESTRIAN, false);
        
        PlainStreetEdge topBack = new PlainStreetEdge(tr, tl, (LineString) top.getGeometry()
                .reverse(), "topBack", 1500, StreetTraversalPermission.ALL, true);
        PlainStreetEdge bottomBack = new PlainStreetEdge(br, bl, (LineString) bottom.getGeometry()
                .reverse(), "bottomBack", 1500, StreetTraversalPermission.ALL, true);
        leftBack = new PlainStreetEdge(tl, bl, (LineString) left.getGeometry().reverse(),
                "leftBack", 1500, StreetTraversalPermission.ALL, true);
        rightBack = new PlainStreetEdge(tr, br, (LineString) right.getGeometry().reverse(),
                "rightBack", 1500, StreetTraversalPermission.ALL, true);

        Stop s1 = new Stop();
        s1.setName("transitVertex 1");
        s1.setLon(-74.005);
        s1.setLat(40.0099999);
        s1.setId(new AgencyAndId("A", "fleem station"));

        Stop s2 = new Stop();
        s2.setName("transitVertex 2");
        s2.setLon(-74.002);
        s2.setLat(40.0099999);
        s2.setId(new AgencyAndId("A", "morx station"));

        station1 = new TransitStop(graph, s1);
        station2 = new TransitStop(graph, s2);
        station1.addMode(TraverseMode.RAIL);
        station2.addMode(TraverseMode.RAIL);
        
        graph.rebuildVertexAndEdgeIndices();
    }

    public void testHalfEdges() {
        // the shortest half-edge from the start vertex takes you down, but the shortest total path
        // is up and over

        int nVertices = graph.getVertices().size();
        int nEdges = graph.getEdges().size();

        RoutingRequest options = new RoutingRequest();

        HashSet<Edge> turns = new HashSet<Edge>();
        turns.add(left);
        turns.add(leftBack);

        StreetLocation start = StreetLocation.createStreetLocation(graph, "start", "start",
                cast(turns, StreetEdge.class),
                new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()));

        HashSet<Edge> endTurns = new HashSet<Edge>();
        endTurns.add(right);
        endTurns.add(rightBack);

        StreetLocation end = StreetLocation.createStreetLocation(graph, "end", "end",
                cast(endTurns, StreetEdge.class),
                new LinearLocation(0, 0.8).getCoordinate(right.getGeometry()));

        assertTrue(start.getX() < end.getX());
        assertTrue(start.getY() < end.getY());

        List<Edge> extra = end.getExtra();

        assertEquals(4, extra.size());

        long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 11, 1, 12, 34, 25);
        options.dateTime = startTime;
        options.setRoutingContext(graph, br, end);
        options.setMaxWalkDistance(Double.MAX_VALUE);
        ShortestPathTree spt1 = aStar.getShortestPathTree(options);

        GraphPath pathBr = spt1.getPath(end, false);
        assertNotNull("There must be a path from br to end", pathBr);

        options.setRoutingContext(graph, tr, end);
        ShortestPathTree spt2 = aStar.getShortestPathTree(options);

        GraphPath pathTr = spt2.getPath(end, false);
        assertNotNull("There must be a path from tr to end", pathTr);
        assertTrue("path from bottom to end must be longer than path from top to end",
                pathBr.getWeight() > pathTr.getWeight());

        options.setRoutingContext(graph, start, end);
        ShortestPathTree spt = aStar.getShortestPathTree(options);

        GraphPath path = spt.getPath(end, false);
        assertNotNull("There must be a path from start to end", path);

        // the bottom is not part of the shortest path
        for (State s : path.states) {
            assertNotSame(s.getVertex(), graph.getVertex("bottom"));
            assertNotSame(s.getVertex(), graph.getVertex("bottomBack"));
        }

        options.setArriveBy(true);
        options.setRoutingContext(graph, start, end);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(start, false);
        assertNotNull("There must be a path from start to end (looking back)", path);

        // the bottom edge is not part of the shortest path
        for (State s : path.states) {
            assertNotSame(s.getVertex(), graph.getVertex("bottom"));
            assertNotSame(s.getVertex(), graph.getVertex("bottomBack"));
        }

        // Number of vertices and edges should be the same as before after a cleanup.
        options.cleanup();
        assertEquals(nVertices, graph.getVertices().size());
        assertEquals(nEdges, graph.getEdges().size());

        /*
         * Now, the right edge is not bikeable. But the user can walk their bike. So here are some tests that prove (a) that walking bikes works, but
         * that (b) it is not preferred to riding a tiny bit longer.
         */

        options = new RoutingRequest(new TraverseModeSet(TraverseMode.BICYCLE));
        start = StreetLocation.createStreetLocation(graph, "start1", "start1",
                cast(turns, StreetEdge.class),
                new LinearLocation(0, 0.95).getCoordinate(top.getGeometry()));
        end = StreetLocation.createStreetLocation(graph, "end1", "end1",
                cast(turns, StreetEdge.class),
                new LinearLocation(0, 0.95).getCoordinate(bottom.getGeometry()));

        options.setRoutingContext(graph, start, end);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(start, false);
        assertNotNull("There must be a path from top to bottom along the right", path);

        // the left edge is not part of the shortest path (even though the bike must be walked along the right)
        for (State s : path.states) {
            assertNotSame(s.getVertex(), graph.getVertex("left"));
            assertNotSame(s.getVertex(), graph.getVertex("leftBack"));
        }

        // Number of vertices and edges should be the same as before after a cleanup.
        options.cleanup();
        assertEquals(nVertices, graph.getVertices().size());
        assertEquals(nEdges, graph.getEdges().size());

        start = StreetLocation.createStreetLocation(graph, "start2", "start2",
                cast(turns, StreetEdge.class),
                new LinearLocation(0, 0.55).getCoordinate(top.getGeometry()));
        end = StreetLocation.createStreetLocation(graph, "end2", "end2",
                cast(turns, StreetEdge.class),
                new LinearLocation(0, 0.55).getCoordinate(bottom.getGeometry()));

        options.setRoutingContext(graph, start, end);
        spt = aStar.getShortestPathTree(options);

        path = spt.getPath(start, false);
        assertNotNull("There must be a path from top to bottom", path);

        // the right edge is not part of the shortest path, e
        for (State s : path.states) {
            assertNotSame(s.getVertex(), graph.getVertex("right"));
            assertNotSame(s.getVertex(), graph.getVertex("rightBack"));
        }

        // Number of vertices and edges should be the same as before after a cleanup.
        options.cleanup();
        assertEquals(nVertices, graph.getVertices().size());
        assertEquals(nEdges, graph.getEdges().size());
    }
    
    public void testRouteToSameEdge() {
        RoutingRequest options = new RoutingRequest();

        HashSet<Edge> turns = new HashSet<Edge>();
        turns.add(left);
        turns.add(leftBack);
        
        StreetLocation start = StreetLocation.createStreetLocation(graph, "start", "start",
                cast(turns, StreetEdge.class),
                new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()));

        StreetLocation end = StreetLocation.createStreetLocation(graph, "end", "end",
                cast(turns, StreetEdge.class),
                new LinearLocation(0, 0.8).getCoordinate(left.getGeometry()));

        assertEquals(start.getX(), end.getX());
        assertTrue(start.getY() < end.getY());

        List<Edge> extra = end.getExtra();

        assertEquals(4, extra.size());

        long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 11, 1, 12, 34, 25);
        options.dateTime = startTime;
        options.setRoutingContext(graph, start, end);
        options.setMaxWalkDistance(Double.MAX_VALUE);
        ShortestPathTree spt = aStar.getShortestPathTree(options);

        GraphPath path = spt.getPath(end, false);
        assertNotNull("There must be a path from start to end", path);        
        assertEquals(1, path.edges.size());
    }

    public void testRouteToSameEdgeBackwards() {
        RoutingRequest options = new RoutingRequest();

        // Sits only on the leftmost edge, not on its reverse.
        HashSet<Edge> turns = new HashSet<Edge>();
        turns.add(left);

        StreetLocation start = StreetLocation.createStreetLocation(graph, "start", "start",
                cast(turns, StreetEdge.class),
                new LinearLocation(0, 0.8).getCoordinate(left.getGeometry()));
        
        StreetLocation end = StreetLocation.createStreetLocation(graph, "end", "end",
                cast(turns, StreetEdge.class),
                new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()));

        assertEquals(start.getX(), end.getX());
        assertTrue(start.getY() > end.getY());

        List<Edge> extra = end.getExtra();
        assertEquals(2, extra.size());

        long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 11, 1, 12, 34, 25);
        options.dateTime = startTime;
        options.setRoutingContext(graph, start, end);
        options.setMaxWalkDistance(Double.MAX_VALUE);
        ShortestPathTree spt = aStar.getShortestPathTree(options);

        GraphPath path = spt.getPath(end, false);
        assertNotNull("There must be a path from start to end", path);        
        assertTrue(path.edges.size() > 1);
    }

    /**
     * Test that alerts on split streets are preserved, i.e. if there are alerts on the street that is split the same alerts should be present on the
     * new street.
     */
    public void testStreetSplittingAlerts() {
        HashSet<Edge> turns = new HashSet<Edge>();
        turns.add(left);
        turns.add(leftBack);

        Set<Alert> alert = new HashSet<Alert>();
        alert.add(Alert.createSimpleAlerts("This is the alert"));

        left.setNote(alert);
        leftBack.setNote(alert);

        StreetLocation start = StreetLocation.createStreetLocation(graph, "start", "start",
                cast(turns, StreetEdge.class),
                new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()));

        // The alert should be preserved
        // traverse the FreeEdge from the StreetLocation to the new IntersectionVertex
        RoutingRequest req = new RoutingRequest();
        req.setMaxWalkDistance(Double.MAX_VALUE);
        State traversedOne = new State((Vertex) start, req);
        State currentState;
        for (Edge e : start.getOutgoing()) {
            currentState = e.traverse(traversedOne);
            if (currentState != null) {
                traversedOne = currentState;
                break;
            }
        }

        assertEquals(alert, traversedOne.getBackAlerts());
        assertNotSame(left, traversedOne.getBackEdge().getFromVertex());
        assertNotSame(leftBack, traversedOne.getBackEdge().getFromVertex());

        // now, make sure wheelchair alerts are preserved
        Set<Alert> wheelchairAlert = new HashSet<Alert>();
        wheelchairAlert.add(Alert.createSimpleAlerts("This is the wheelchair alert"));

        left.setNote(null);
        leftBack.setNote(null);
        left.setWheelchairNote(wheelchairAlert);
        leftBack.setWheelchairNote(wheelchairAlert);

        req.setWheelchairAccessible(true);

        start = StreetLocation.createStreetLocation(graph, "start", "start",
                cast(turns, StreetEdge.class),
                new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()));

        traversedOne = new State((Vertex) start, req);
        for (Edge e : start.getOutgoing()) {
            currentState = e.traverse(traversedOne);
            if (currentState != null) {
                traversedOne = currentState;
                break;
            }
        }

        assertEquals(wheelchairAlert, traversedOne.getBackAlerts());
        assertNotSame(left, traversedOne.getBackEdge().getFromVertex());
        assertNotSame(leftBack, traversedOne.getBackEdge().getFromVertex());
    }

    public void testStreetLocationFinder() {
        StreetVertexIndexServiceImpl finder = new StreetVertexIndexServiceImpl(graph);
        finder.setup();
        // test that the local stop finder finds stops
        GenericLocation loc = new GenericLocation(40.01, -74.005000001);
        assertTrue(finder.getLocalTransitStops(loc.getCoordinate(), 100).size() > 0);

        // test that the closest vertex finder returns the closest vertex
        StreetLocation some = (StreetLocation) finder.getVertexForLocation(new GenericLocation(
                40.00, -74.00), null);
        assertNotNull(some);

        // test that the closest vertex finder correctly splits streets
        StreetLocation start = (StreetLocation) finder.getVertexForLocation(new GenericLocation(
                40.004, -74.01), null);
        assertNotNull(start);
        assertTrue("wheelchair accessibility is correctly set (splitting)",
                start.isWheelchairAccessible());

        List<Edge> extras = start.getExtra();
        assertEquals(4, extras.size());

        RoutingRequest biking = new RoutingRequest(new TraverseModeSet(TraverseMode.BICYCLE));
        StreetLocation end = (StreetLocation) finder.getVertexForLocation(new GenericLocation(
                40.008, -74.0), biking);
        assertNotNull(end);

        extras = end.getExtra();
        assertEquals(4, extras.size());

        // test that the closest vertex finder also adds an edge to transit
        // stops (if you are really close to the transit stop relative to the
        // street)
        StreetLocation location = (StreetLocation) finder.getVertexForLocation(new GenericLocation(
                40.00999, -74.004999), new RoutingRequest());
        assertTrue(location.isWheelchairAccessible());
        boolean found = false;
        for (Edge extra : location.getExtra()) {
            if (extra instanceof FreeEdge && ((FreeEdge) extra).getToVertex().equals(station1)) {
                found = true;
            }
        }
        assertTrue(found);

        // test that it is possible to travel between two splits on the same street
        RoutingRequest walking = new RoutingRequest(TraverseMode.WALK);
        start = (StreetLocation) finder.getVertexForLocation(new GenericLocation(40.004, -74.0),
                walking);
        end = (StreetLocation) finder.getVertexForLocation(new GenericLocation(40.008, -74.0),
                walking);
        // , start.getExtra());
        assertNotNull(end);
        walking.setRoutingContext(graph, start, end);
        ShortestPathTree spt = aStar.getShortestPathTree(walking);
        GraphPath path = spt.getPath(end, false);
        for (State s : path.states) {
            assertFalse(s.getVertex() == top);
        }
    }

    public void testNetworkLinker() {
        int numVerticesBefore = graph.getVertices().size();
        NetworkLinker nl = new NetworkLinker(graph);
        nl.createLinkage();
        int numVerticesAfter = graph.getVertices().size();
        assertEquals(4, numVerticesAfter - numVerticesBefore);
        Collection<Edge> outgoing = station1.getOutgoing();
        assertTrue(outgoing.size() == 2);
        Edge edge = outgoing.iterator().next();

        Vertex midpoint = edge.getToVertex();
        assertTrue(Math.abs(midpoint.getCoordinate().y - 40.01) < 0.00000001);

        outgoing = station2.getOutgoing();
        assertTrue(outgoing.size() == 2);
        edge = outgoing.iterator().next();

        Vertex station2point = edge.getToVertex();
        assertTrue(Math.abs(station2point.getCoordinate().x - -74.002) < 0.00000001);

    }
}
