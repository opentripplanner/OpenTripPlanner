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

import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.core.Intersection;
import org.opentripplanner.routing.core.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;

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
        // a 0.1 degree x 0.1 degree square
        Vertex tl = graph.addVertex(new Intersection("tl", -74.1, 40.1));
        Vertex tr = graph.addVertex(new Intersection("tr", -74.0, 40.1));
        Vertex bl = graph.addVertex(new Intersection("bl", -74.1, 40.0));
        Vertex br = graph.addVertex(new Intersection("br", -74.0, 40.0));

        graph.addVertex(new TransitStop("transitVertex", -74.05, 40.1, "transitVertex", "fleem station", null));

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
        rightUp.setGeometry(createGeometry(br, tr));
        graph.addEdge(rightUp);
    }

    public void testHalfEdges() {
        // the shortest half-edge from the start vertex takes you down, but the shortest total path
        // is up and over

        StreetLocation start = StreetLocation.createStreetLocation("start", leftUp, new LinearLocation(0, 0.4));
        StreetLocation end = StreetLocation.createStreetLocation("end", rightUp, new LinearLocation(0, 0.8));

        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 1, 12, 34, 25);

        ShortestPathTree spt = AStar.getShortestPathTree(graph, start, end, new State(startTime
                .getTimeInMillis()), new TraverseOptions());

        GraphPath path = spt.getPath(end);
        assertNotNull(path);

        // the bottom-left point is not part of the shortest path
        for (SPTVertex v : path.vertices) {
            assertNotSame(v.mirror, graph.getVertex("bl"));
        }

    }

    public void testHalfEdgesBack() {
        StreetLocation start = StreetLocation.createStreetLocation("start", leftUp, new LinearLocation(0, 0.4));
        StreetLocation end = StreetLocation.createStreetLocation("end", rightUp, new LinearLocation(0, 0.8));

        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 1, 12, 34, 25);

        ShortestPathTree spt = AStar.getShortestPathTreeBack(graph, start, end, new State(startTime
                .getTimeInMillis()), new TraverseOptions());

        GraphPath path = spt.getPath(start);
        assertNotNull(path);

        // the bottom-left point is not part of the shortest path
        for (SPTVertex v : path.vertices) {
            assertNotSame(v.mirror, graph.getVertex("bl"));
        }

    }

    
    public void testStreetLocationFinder() {
        StreetVertexIndexServiceImpl finder = new StreetVertexIndexServiceImpl(graph);
        finder.setup();

        StreetLocation start = (StreetLocation) finder.getClosestVertex(new Coordinate(-74.1, 40.04));
        assertNotNull(start);
        StreetLocation end = (StreetLocation) finder.getClosestVertex(new Coordinate(-74.0, 40.08));
        assertNotNull(end);

        if (start.streets.get(0) == leftUp) {
            assertTrue(Math.abs(start.location.getSegmentFraction() - 0.4) < 0.00001);
        } else {
            assertTrue(Math.abs(start.location.getSegmentFraction() - 0.6) < 0.00001);
            assertEquals(leftDown, start.streets.get(0));
        }
        assertTrue(start.getDegreeOut() == 2);

        if (end.streets.get(0) == rightUp) {
            assertTrue(Math.abs(end.location.getSegmentFraction()) - 0.2 < 0.00001);
        } else {
            assertTrue(Math.abs(end.location.getSegmentFraction()) - 0.8 < 0.00001);
            assertEquals(rightDown, end.streets.get(0));
        }
        assertTrue(end.getDegreeIn() == 2);

        assertTrue(finder.getClosestVertex(new Coordinate(-74.05000001, 40.1)) instanceof TransitStop);
    }
}
