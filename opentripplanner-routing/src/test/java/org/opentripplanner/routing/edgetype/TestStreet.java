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

package org.opentripplanner.routing.edgetype;

import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextypes.Intersection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class TestStreet extends TestCase {

    private static GeometryFactory _geomFactory = new GeometryFactory();

    public void testStreetWalk() {
        Graph gg = new Graph();

        Vertex start = gg.addVertex("start", -74.002, 40.5);
        Vertex end = gg.addVertex("end", -74.004, 40.5);

        TraverseOptions wo = new TraverseOptions();
        wo.speed = ConstantsForTests.WALKING_SPEED;
        double streetLength = 100; // meters
        Edge ee = new Street(start, end, streetLength);
        gg.addEdge(ee);

        // Start at October 21, 2009 at 1:00:00pm
        GregorianCalendar startTime = new GregorianCalendar(2009, 9, 21, 13, 0, 0);
        GregorianCalendar endTime = (GregorianCalendar) startTime.clone();
        int expectedSecElapsed = (int) (streetLength / wo.speed);
        endTime.add(GregorianCalendar.SECOND, expectedSecElapsed);

        State s0 = new State(startTime.getTimeInMillis());
        TraverseResult wr = ee.traverse(s0, wo);

        assertNotNull(wr);
        assertEquals(wr.weight, streetLength / wo.speed);
        // Has the time elapsed as expected?
        assertEquals(wr.state.getTime(), endTime.getTimeInMillis());

        wr = null;
        s0 = new State(endTime.getTimeInMillis());
        wr = ee.traverseBack(s0, wo);

        assertNotNull(wr);
        assertEquals(wr.weight, streetLength / wo.speed);
        assertEquals(wr.state.getTime(), startTime.getTimeInMillis());
    }

    public void testStreetDirection() {

        Street streetN = createStreet(47.670300419806246, -122.28909730911255, 47.67212102588157,
                -122.28914022445679);
        assertEquals("north", streetN.getDirection());

        Street streetNE = createStreet(47.670300419806246, -122.28909730911255, 47.67209212786854,
                -122.28796005249023);
        assertEquals("northeast", streetNE.getDirection());

        Street streetE = createStreet(47.670300419806246, -122.28909730911255, 47.670285970297634,
                -122.28796005249023);
        assertEquals("east", streetE.getDirection());

        Street streetSE = createStreet(47.670300419806246, -122.28909730911255, 47.66850865023101,
                -122.28802442550659);
        assertEquals("southeast", streetSE.getDirection());

        Street streetS = createStreet(47.670300419806246, -122.28909730911255, 47.668494200226334,
                -122.28914022445679);
        assertEquals("south", streetS.getDirection());

        Street streetSW = createStreet(47.670300419806246, -122.28909730911255, 47.66855200022105,
                -122.2901701927185);
        assertEquals("southwest", streetSW.getDirection());

        Street streetW = createStreet(47.670300419806246, -122.28909730911255, 47.670300419806246,
                -122.29019165039062);
        assertEquals("west", streetW.getDirection());

        Street streetNW = createStreet(47.670300419806246, -122.28909730911255, 47.67214992387863,
                -122.29019165039062);
        assertEquals("northwest", streetNW.getDirection());
    }

    private Street createStreet(double latFrom, double lonFrom, double latTo, double lonTo) {
        Vertex from = new GenericVertex("from", lonFrom, latFrom);
        Vertex to = new GenericVertex("to", lonTo, latTo);
        double len = DistanceLibrary.distance(latFrom, lonFrom, latTo, lonTo);
        Street street = new Street(from, to, len);
        LineString line = _geomFactory.createLineString(new Coordinate[] { from.getCoordinate(),
                to.getCoordinate() });
        street.setGeometry(line);
        return street;
    }

    public void testMaxWalkDistance() {
        /* create a square */

        Graph graph = new Graph();
        // a 1 degree x 1 degree square, right edge missing
        Vertex tl = graph.addVertex(new GenericVertex("tl", -74, 41, "tl", Intersection.class));
        Vertex tr = graph.addVertex(new GenericVertex("tr", -73, 41, "tr", Intersection.class));
        Vertex bl = graph.addVertex(new GenericVertex("bl", -74, 40, "bl", Intersection.class));
        Vertex br = graph.addVertex(new GenericVertex("br", -73, 40, "br", Intersection.class));

        Street top = new Street(tl, tr, 20000);
        top.setGeometry(createGeometry(tl, tr));
        graph.addEdge(top);

        Street bottom = new Street(bl, br, 20000);
        bottom.setGeometry(createGeometry(bl, br));
        graph.addEdge(bottom);

        Street left = new Street(bl, tl, 20000);
        left.setGeometry(createGeometry(bl, tl));
        graph.addEdge(left);

        // now a very slow transfer edge spanning the right edge of the square
        Transfer transfer = new Transfer(br, tr, 99999);
        graph.addEdge(transfer);

        // with no maxWalkDistance, the transfer will not be taken

        TraverseOptions options = new TraverseOptions();
        ShortestPathTree spt = AStar.getShortestPathTree(graph, bl, tr, new State(0), options);

        GraphPath path = spt.getPath(tr);
        assertNotNull(path);

        boolean found = false;
        for (SPTVertex v : path.vertices) {
            if (v.mirror == br) {
                found = true;
            }
        }
        assertFalse(found);

        // with a maxWalkDistance, the transfer will be taken.
        options.maxWalkDistance = 10000;
        spt = AStar.getShortestPathTree(graph, bl, tr, new State(0), options);

        path = spt.getPath(tr);
        assertNotNull(path);

        found = false;
        for (SPTVertex v : path.vertices) {
            if (v.mirror == br) {
                found = true;
            }
        }
        assertTrue(found);
    }

    private LineString createGeometry(Vertex a, Vertex b) {

        GeometryFactory factory = new GeometryFactory();
        Coordinate[] cs = new Coordinate[2];
        cs[0] = a.getCoordinate();
        cs[1] = b.getCoordinate();
        return factory.createLineString(cs);
    }
}
