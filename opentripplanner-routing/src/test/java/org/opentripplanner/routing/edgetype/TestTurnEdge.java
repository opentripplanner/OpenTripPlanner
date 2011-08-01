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
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import com.vividsolutions.jts.geom.Geometry;

public class TestTurnEdge extends TestCase {

    public void testStreetSpeed() {
        assertTrue(Math.abs(StreetVertex.slopeSpeedCoefficient(0,0) - 1) < 0.01);
        assertTrue(Math.abs(StreetVertex.slopeSpeedCoefficient(0.35, 0) - 0.0624) < 0.02);
        assertTrue(Math.abs(StreetVertex.slopeSpeedCoefficient(0, 5000) - 1.17) < 0.03);
    }

    public void testStreetWalk() {
        Graph gg = new Graph();

        double streetLength = 100;
        
        StreetVertex start = new StreetVertex("start", GeometryUtils.makeLineString(-74.002, 40.5, -74.004, 40.5, -74.004, 40.5, -74.006, 41.0), "start", streetLength, false, null);
        StreetVertex end = new StreetVertex("end", GeometryUtils.makeLineString(-74.004, 40.5, -74.006, 41.0), "end", streetLength, false, null);
        
        gg.addVertex(start);
        gg.addVertex(end);

        TraverseOptions options = new TraverseOptions();
        options.speed = ConstantsForTests.WALKING_SPEED;
        
        TurnEdge ee = new TurnEdge(start, end);
        gg.addEdge(ee);

        // Start at October 21, 2009 at 1:00:00pm
        GregorianCalendar startTime = new GregorianCalendar(2009, 9, 21, 13, 0, 0);
        GregorianCalendar endTime = (GregorianCalendar) startTime.clone();
        int expectedSecElapsed = (int) (streetLength / options.speed);
        endTime.add(GregorianCalendar.SECOND, expectedSecElapsed);

        State s0 = new State(startTime.getTimeInMillis(), start, options);
        State s1 = ee.traverse(s0);

        assertNotNull(s1);
        assertTrue(Math.abs(s1.getWeight() -  options.walkReluctance * streetLength / options.speed) < 10); //they're not identical because of the turn cost
        // Has the time elapsed as expected?
        assertTrue(Math.abs(s1.getTime() - endTime.getTimeInMillis()) < 10000);

        options.setArriveBy(true);
        s0 = new State(endTime.getTimeInMillis(), end, options);
        s1 = ee.traverse(s0);

        assertNotNull(s1);
        assertTrue(Math.abs(s1.getWeight() -  options.walkReluctance * streetLength / options.speed) < 10);
        assertTrue(Math.abs(s1.getTime() - startTime.getTimeInMillis()) < 10000);
    }

    public void testMaxWalkDistance() {
        /* create a square */

        Graph graph = new Graph();
        // a 1 degree x 1 degree square, right edge missing
        StreetVertex top = new StreetVertex("top", GeometryUtils.makeLineString(-74.1, 40.1, -74.0, 40.1), "top", 10000, false, null);
        StreetVertex bottom = new StreetVertex("bottom", GeometryUtils.makeLineString(-74.1, 40.0, -74.0, 40.0), "bottom", 10000, false, null);
        StreetVertex left = new StreetVertex("left", GeometryUtils.makeLineString(-74.1, 40.0, -74.1, 40.1), "left", 10000, false, null);
        StreetVertex right = new StreetVertex("right", GeometryUtils.makeLineString(-74.0, 40.0, -74.0, 40.1), "right", 10000, false, null);
        
        StreetVertex topBack = new StreetVertex("topBack", GeometryUtils.makeLineString(-74.0, 40.1, -74.1, 40.1), "topBack", 10000, true, null);
        StreetVertex bottomBack = new StreetVertex("bottomBack", GeometryUtils.makeLineString(-74.0, 40.0, -74.1, 40.0), "bottomBack", 10000, true, null);
        StreetVertex leftBack = new StreetVertex("leftBack", GeometryUtils.makeLineString(-74.0, 40.1, -74.1, 40.0), "leftBack", 10000, true, null);
        StreetVertex rightBack = new StreetVertex("rightBack", GeometryUtils.makeLineString(-74.0, 40.1, -74.0, 40.0), "rightBack", 10000, true, null);
        
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
        Vertex trOut = graph.addVertex(new EndpointVertex("tr out", -74.0, 40.1));
        Vertex blOut = graph.addVertex(new EndpointVertex("bl out", -74.0, 40.0));
        Vertex brOut = graph.addVertex(new EndpointVertex("br out", -74.1, 40.0));
        
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


        // now a very slow set of transfer edges between the bottom and the target
        graph.addEdge(new MockTransfer(bottom, trIn, 99999));
        graph.addEdge(new MockTransfer(bottomBack, trIn, 99999));

        for (GraphVertex gv : graph.getVertices()) {
            //set distance to nearest transit stop
            gv.vertex.setDistanceToNearestTransitStop(0);
        }
        
        // with no maxWalkDistance, the transfer will not be taken

        TraverseOptions options = new TraverseOptions();
        ShortestPathTree spt = AStar.getShortestPathTree(graph, blOut, trIn, 0, options);

        GraphPath path = spt.getPath(trIn, false);
        assertNotNull(path);

        boolean found = false;
        for (State s : path.states) {
            if (s.getVertex() == bottom || s.getVertex() == bottomBack) {
                found = true;
            }
        }
        assertFalse(found);

        // with a maxWalkDistance, the transfer will be taken.
        options.setMaxWalkDistance(10000);
        spt = AStar.getShortestPathTree(graph, blOut, trIn, 0, options);

        path = spt.getPath(trIn, false);
        assertNotNull(path);

        found = false;
        for (State s : path.states) {
            if (s.getVertex() == bottom || s.getVertex() == bottomBack) {
                found = true;
            }
        }
        assertTrue(found);
    }

}

class MockTransfer extends AbstractEdge {

    private static final long serialVersionUID = 1L;
    private int cost;

    public MockTransfer(Vertex fromv, Vertex tov, int cost) {
        super(fromv, tov);
        this.cost = cost;
    }

    @Override
    public double getDistance() {
        return 0;
    }

    @Override
    public Geometry getGeometry() {
        return null;
    }

    @Override
    public TraverseMode getMode() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public State traverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(cost);
        s1.incrementWeight(cost);
        return s1.makeState();
    }

}