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

import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.edgetype.EndpointVertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.edgetype.loader.NetworkLinker;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;

public class TestHalfEdges extends TestCase {

    Graph graph;

    private StreetVertex top;

    private StreetVertex bottom;

    private StreetVertex left;

    private StreetVertex right;

    private StreetVertex leftBack;

    private StreetVertex rightBack;

    private Vertex brOut;

    private Vertex trOut;

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
        top = new StreetVertex("top", GeometryUtils.makeLineString(-74.1, 40.1, -74.0, 40.1), "top", 1500, false);
        bottom = new StreetVertex("bottom", GeometryUtils.makeLineString(-74.1, 40.0, -74.0, 40.0), "bottom", 1500, false);
        left = new StreetVertex("left", GeometryUtils.makeLineString(-74.1, 40.0, -74.1, 40.1), "left", 1500, false);
        right = new StreetVertex("right", GeometryUtils.makeLineString(-74.0, 40.0, -74.0, 40.1), "right", 1500, false);
        
        StreetVertex topBack = new StreetVertex("topBack", GeometryUtils.makeLineString(-74.0, 40.1, -74.1, 40.1), "topBack", 1500, true);
        StreetVertex bottomBack = new StreetVertex("bottomBack", GeometryUtils.makeLineString(-74.0, 40.0, -74.1, 40.0), "bottomBack", 1500, true);
        leftBack = new StreetVertex("leftBack", GeometryUtils.makeLineString(-74.1, 40.1, -74.1, 40.0), "leftBack", 1500, true);
        rightBack = new StreetVertex("rightBack", GeometryUtils.makeLineString(-74.0, 40.1, -74.0, 40.0), "rightBack", 1500, true);

        right.setPermission(StreetTraversalPermission.PEDESTRIAN);
        
        graph.addVertex(top);
        graph.addVertex(bottom);
        graph.addVertex(left);
        graph.addVertex(right);
        
        graph.addVertex(topBack);
        graph.addVertex(bottomBack);
        graph.addVertex(leftBack);
        graph.addVertex(rightBack);
        
        EndpointVertex tlIn = (EndpointVertex) graph.addVertex(new EndpointVertex("tl in", -74.1, 40.1));
        EndpointVertex trIn = (EndpointVertex) graph.addVertex(new EndpointVertex("tr in", -74.0, 40.1));
        EndpointVertex blIn = (EndpointVertex) graph.addVertex(new EndpointVertex("bl in", -74.0, 40.0));
        EndpointVertex brIn = (EndpointVertex) graph.addVertex(new EndpointVertex("br in", -74.1, 40.0));

        Vertex tlOut = graph.addVertex(new EndpointVertex("tl out", -74.1, 40.1));
        trOut = graph.addVertex(new EndpointVertex("tr out", -74.0, 40.1));
        Vertex blOut = graph.addVertex(new EndpointVertex("bl out", -74.0, 40.0));
        brOut = graph.addVertex(new EndpointVertex("br out", -74.1, 40.0));
        
        graph.addEdge(new FreeEdge(tlOut, top));
        graph.addEdge(new FreeEdge(tlOut, leftBack));
        
        graph.addEdge(new FreeEdge(trOut, topBack));
        graph.addEdge(new FreeEdge(trOut, rightBack));
        
        graph.addEdge(new FreeEdge(blOut, bottom));
        graph.addEdge(new FreeEdge(blOut, left));
        
        graph.addEdge(new FreeEdge(brOut, bottomBack));
        graph.addEdge(new FreeEdge(brOut, right));
        
        graph.addEdge(new OutEdge(topBack, tlIn));
        graph.addEdge(new OutEdge(left, tlIn));
        
        graph.addEdge(new OutEdge(top, trIn));
        graph.addEdge(new OutEdge(right, trIn));
        
        graph.addEdge(new OutEdge(bottomBack, blIn));
        graph.addEdge(new OutEdge(leftBack, blIn));
        
        graph.addEdge(new OutEdge(bottom, brIn));
        graph.addEdge(new OutEdge(rightBack, brIn));      
        
        graph.addEdge(new TurnEdge(top, rightBack));
        graph.addEdge(new TurnEdge(rightBack, bottomBack));
        graph.addEdge(new TurnEdge(bottomBack, left));
        graph.addEdge(new TurnEdge(left, top));
        
        graph.addEdge(new TurnEdge(topBack, leftBack));
        graph.addEdge(new TurnEdge(leftBack, bottom));
        graph.addEdge(new TurnEdge(bottom, right));
        graph.addEdge(new TurnEdge(right, topBack));
        
        station1 = new TransitStop("transitVertex 1", -74.05, 40.099999, "transitVertex 1", "fleem station", null);
        graph.addVertex(station1);
        station2 = new TransitStop("transitVertex 2", -74.02, 40.099999, "transitVertex 2", "morx station", null);
        graph.addVertex(station2);
    }

    public void testHalfEdges() {
        // the shortest half-edge from the start vertex takes you down, but the shortest total path
        // is up and over

        HashSet<Edge> turns = new HashSet<Edge>(graph.getOutgoing(left));
        turns.addAll(graph.getOutgoing(leftBack));
        
        StreetLocation start = StreetLocation.createStreetLocation(graph, "start", "start", turns, new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()));

        HashSet<Edge> endTurns = new HashSet<Edge>(graph.getOutgoing(right));
        endTurns.addAll(graph.getOutgoing(rightBack));
        
        StreetLocation end = StreetLocation.createStreetLocation(graph, "end", "end", endTurns, new LinearLocation(0, 0.8).getCoordinate(right.getGeometry()));
        
        assertTrue(start.getX() < end.getX());
        assertTrue(start.getY() < end.getY());
        
        List<Edge> extra = end.getExtra();
        
        assertEquals(12, extra.size());
        
        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 1, 12, 34, 25);

        ShortestPathTree spt1 = AStar.getShortestPathTree(graph, brOut, end, new State(startTime
                .getTimeInMillis()), new TraverseOptions());

        GraphPath pathBr = spt1.getPath(end);
        assertNotNull("There must be a path from br to end", pathBr);
        
        ShortestPathTree spt2 = AStar.getShortestPathTree(graph, trOut, end, new State(startTime
                .getTimeInMillis()), new TraverseOptions());

        GraphPath pathTr = spt2.getPath(end);
        assertNotNull("There must be a path from tr to end", pathTr);
        assertTrue("path from bottom to end must be longer than path from top to end", pathBr.vertices.lastElement().weightSum > pathTr.vertices.lastElement().weightSum);
        
        ShortestPathTree spt = AStar.getShortestPathTree(graph, start, end, new State(startTime
                .getTimeInMillis()), new TraverseOptions());

        GraphPath path = spt.getPath(end);
        assertNotNull("There must be a path from start to end", path);

        // the bottom is not part of the shortest path
        for (SPTVertex v : path.vertices) {
            assertNotSame(v.mirror, graph.getVertex("bottom"));
            assertNotSame(v.mirror, graph.getVertex("bottomBack"));
        }

        startTime = new GregorianCalendar(2009, 11, 1, 12, 34, 25);

        TraverseOptions options = new TraverseOptions();
        options.setArriveBy(true);
        spt = AStar.getShortestPathTreeBack(graph, start, end, new State(startTime
                .getTimeInMillis()), options);

        path = spt.getPath(start);
        assertNotNull("There must be a path from start to end (looking back)", path);

        // the bottom edge is not part of the shortest path
        for (SPTVertex v : path.vertices) {
            assertNotSame(v.mirror, graph.getVertex("bottom"));
            assertNotSame(v.mirror, graph.getVertex("bottomBack"));
        }

    }

    public void testStreetLocationFinder() {
        StreetVertexIndexServiceImpl finder = new StreetVertexIndexServiceImpl(graph);
        finder.setup();

        assertTrue(finder.getLocalTransitStops(new Coordinate(-74.05000001, 40.1), 100).size() > 0);
        
        StreetLocation start = (StreetLocation) finder.getClosestVertex(new Coordinate(-74.1, 40.04), null);
        assertNotNull(start);

        List<Edge> extra = start.getExtra();
        assertEquals(10, extra.size());
        
        TraverseOptions biking = new TraverseOptions(new TraverseModeSet(TraverseMode.BICYCLE));
        StreetLocation end = (StreetLocation) finder.getClosestVertex(new Coordinate(-74.0, 40.08), biking);
        assertNotNull(end);
        
        extra = end.getExtra();
        //bikes can only go on the back edge
        assertEquals(5, extra.size());
        
    }
    
    public void testNetworkLinker() {
        int numVerticesBefore = graph.getVertices().size();
        NetworkLinker nl = new NetworkLinker(graph);
        nl.createLinkage();
        int numVerticesAfter = graph.getVertices().size();
        assertEquals (4, numVerticesAfter - numVerticesBefore);
        Collection<Edge> outgoing = graph.getOutgoing(station1);
        assertTrue(outgoing.size() == 1);
        Edge edge = outgoing.iterator().next();
        
        Vertex midpoint = edge.getToVertex();
        assertTrue(Math.abs(midpoint.getCoordinate().y - 40.1) < 0.00000001);
        
        outgoing = graph.getOutgoing(station2);
        assertTrue(outgoing.size() == 1);
        edge = outgoing.iterator().next();
        
        Vertex station2point = edge.getToVertex();
        assertTrue(Math.abs(station2point.getCoordinate().x - -74.02) < 0.00000001);
        
    }
}
