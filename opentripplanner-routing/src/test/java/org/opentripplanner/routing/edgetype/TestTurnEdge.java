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
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.opentripplanner.util.TestUtils;

import com.vividsolutions.jts.geom.Geometry;

public class TestTurnEdge extends TestCase {

    public void testStreetSpeed() {
        assertTrue(Math.abs(ElevationUtils.slopeSpeedCoefficient(0,0) - 1) < 0.01);
        assertTrue(Math.abs(ElevationUtils.slopeSpeedCoefficient(0.35, 0) - 0.0624) < 0.02);
        assertTrue(Math.abs(ElevationUtils.slopeSpeedCoefficient(0, 5000) - 1.17) < 0.03);
    }

    public void testStreetWalk() {
        Graph gg = new Graph();

        double streetLength = 100;
        
        TurnVertex start = new TurnVertex(gg, "start", GeometryUtils.makeLineString(-74.002, 40.5, -74.004, 40.5, -74.004, 40.5, -74.006, 41.0), "start", streetLength, false, null);
        TurnVertex end = new TurnVertex(gg, "end", GeometryUtils.makeLineString(-74.004, 40.5, -74.006, 41.0), "end", streetLength, false, null);
        
        TraverseOptions options = new TraverseOptions();
        options.setWalkSpeed(ConstantsForTests.WALKING_SPEED);
        
        TurnEdge ee = new TurnEdge(start, end);

        // Start at October 21, 2009 at 1:00:00pm
        GregorianCalendar startTime = new GregorianCalendar(2009, 9, 21, 13, 0, 0);
        GregorianCalendar endTime = (GregorianCalendar) startTime.clone();
        int expectedSecElapsed = (int) (streetLength / options.getSpeed(TraverseMode.WALK));
        endTime.add(GregorianCalendar.SECOND, expectedSecElapsed);

        State s0 = new State(TestUtils.toSeconds(startTime), start, options);
        State s1 = ee.traverse(s0);

        assertNotNull(s1);
        assertTrue(Math.abs(s1.getWeight() -  options.walkReluctance * streetLength / options.getSpeed(TraverseMode.WALK)) < 10); //they're not identical because of the turn cost
        // Has the time elapsed as expected?
        assertTrue(Math.abs(s1.getTime() - endTime.getTimeInMillis() / 1000) < 10);

        options.setArriveBy(true);
        s0 = new State(TestUtils.toSeconds(endTime), end, options);
        s1 = ee.traverse(s0);

        assertNotNull(s1);
        assertTrue(Math.abs(s1.getWeight() -  options.walkReluctance * streetLength / options.getSpeed(TraverseMode.WALK)) < 10);
        assertTrue(Math.abs(s1.getTime() - startTime.getTimeInMillis() / 1000) < 10);
    }

    public void testMaxWalkDistance() {
        /* create a square */

        Graph graph = new Graph();
        // a 1 degree x 1 degree square, right edge missing
        TurnVertex top = new TurnVertex(graph, "top", GeometryUtils.makeLineString(-74.1, 40.1, -74.0, 40.1), "top", 10000, false, null);
        TurnVertex bottom = new TurnVertex(graph, "bottom", GeometryUtils.makeLineString(-74.1, 40.0, -74.0, 40.0), "bottom", 10000, false, null);
        TurnVertex left = new TurnVertex(graph, "left", GeometryUtils.makeLineString(-74.1, 40.0, -74.1, 40.1), "left", 10000, false, null);
        TurnVertex right = new TurnVertex(graph, "right", GeometryUtils.makeLineString(-74.0, 40.0, -74.0, 40.1), "right", 10000, false, null);
        
        TurnVertex topBack = new TurnVertex(graph, "topBack", GeometryUtils.makeLineString(-74.0, 40.1, -74.1, 40.1), "topBack", 10000, true, null);
        TurnVertex bottomBack = new TurnVertex(graph, "bottomBack", GeometryUtils.makeLineString(-74.0, 40.0, -74.1, 40.0), "bottomBack", 10000, true, null);
        TurnVertex leftBack = new TurnVertex(graph, "leftBack", GeometryUtils.makeLineString(-74.0, 40.1, -74.1, 40.0), "leftBack", 10000, true, null);
        TurnVertex rightBack = new TurnVertex(graph, "rightBack", GeometryUtils.makeLineString(-74.0, 40.1, -74.0, 40.0), "rightBack", 10000, true, null);
        
        IntersectionVertex tlIn = new IntersectionVertex(graph, "tl in", -74.1, 40.1);
        IntersectionVertex trIn = new IntersectionVertex(graph, "tr in", -74.0, 40.1);
        IntersectionVertex blIn = new IntersectionVertex(graph, "bl in", -74.0, 40.0);
        IntersectionVertex brIn = new IntersectionVertex(graph, "br in", -74.1, 40.0);

        Vertex tlOut = new IntersectionVertex(graph, "tl out", -74.1, 40.1);
        Vertex trOut = new IntersectionVertex(graph, "tr out", -74.0, 40.1);
        Vertex blOut = new IntersectionVertex(graph, "bl out", -74.0, 40.0);
        Vertex brOut = new IntersectionVertex(graph, "br out", -74.1, 40.0);
        
        new FreeEdge(tlOut, top);
        new FreeEdge(tlOut, leftBack);
        
        new FreeEdge(trOut, topBack);
        new FreeEdge(trOut, rightBack);
        
        new FreeEdge(blOut, bottom);
        new FreeEdge(blOut, left);
        
        new FreeEdge(brOut, bottomBack);
        new FreeEdge(brOut, right);
        
        new OutEdge(topBack, tlIn);
        new OutEdge(left, tlIn);
        
        new OutEdge(top, trIn);
        new OutEdge(right, trIn);
        
        new OutEdge(bottomBack, blIn);
        new OutEdge(leftBack, blIn);
        
        new OutEdge(bottom, brIn);
        new OutEdge(rightBack, brIn);      
        
        new TurnEdge(top, rightBack);
        new TurnEdge(rightBack, bottomBack);
        new TurnEdge(bottomBack, left);
        new TurnEdge(left, top);
        
        new TurnEdge(topBack, leftBack);
        new TurnEdge(leftBack, bottom);
        new TurnEdge(bottom, right);
        new TurnEdge(right, topBack);


        // now a very slow set of transfer edges between the bottom and the target
        new MockTransfer(bottom, trIn, 99999);
        new MockTransfer(bottomBack, trIn, 99999);

        for (Vertex gv : graph.getVertices()) {
            //set distance to nearest transit stop
            gv.setDistanceToNearestTransitStop(0);
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