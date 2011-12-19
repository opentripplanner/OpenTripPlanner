/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.algorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.graph.AbstractVertex;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class AStarTest {

    private Graph _graph;

    @Before
    public void before() {

        _graph = new Graph();

        vertex("56th_24th", 47.669457, -122.387577);
        vertex("56th_22nd", 47.669462, -122.384739);
        vertex("56th_20th", 47.669457, -122.382106);

        vertex("market_24th", 47.668690, -122.387577);
        vertex("market_ballard", 47.668683, -122.386096);
        vertex("market_22nd", 47.668686, -122.384749);
        vertex("market_leary", 47.668669, -122.384392);
        vertex("market_russell", 47.668655, -122.382997);
        vertex("market_20th", 47.668684, -122.382117);

        vertex("shilshole_24th", 47.668419, -122.387534);
        vertex("shilshole_22nd", 47.666519, -122.384744);
        vertex("shilshole_vernon", 47.665938, -122.384048);
        vertex("shilshole_20th", 47.664356, -122.382192);

        vertex("ballard_turn", 47.668509, -122.386069);
        vertex("ballard_22nd", 47.667624, -122.384744);
        vertex("ballard_vernon", 47.666422, -122.383158);
        vertex("ballard_20th", 47.665476, -122.382128);

        vertex("leary_vernon", 47.666863, -122.382353);
        vertex("leary_20th", 47.666682, -122.382160);

        vertex("russell_20th", 47.667846, -122.382128);

        edges("56th_24th", "56th_22nd", "56th_20th");

        edges("56th_24th", "market_24th");
        edges("56th_22nd", "market_22nd");
        edges("56th_20th", "market_20th");

        edges("market_24th", "market_ballard", "market_22nd", "market_leary", "market_russell",
                "market_20th");
        edges("market_24th", "shilshole_24th", "shilshole_22nd", "shilshole_vernon",
                "shilshole_20th");
        edges("market_ballard", "ballard_turn", "ballard_22nd", "ballard_vernon", "ballard_20th");
        edges("market_leary", "leary_vernon", "leary_20th");
        edges("market_russell", "russell_20th");

        edges("market_22nd", "ballard_22nd", "shilshole_22nd");
        edges("leary_vernon", "ballard_vernon", "shilshole_vernon");
        edges("market_20th", "russell_20th", "leary_20th", "ballard_20th", "shilshole_20th");

    }

    @Test
    public void testForward() {

        TraverseOptions options = new TraverseOptions();
        options.speed = 1.0;

        ShortestPathTree tree = AStar.getShortestPathTree(_graph, "56th_24th", "leary_20th",
        		0, options);

        GraphPath path = tree.getPath(_graph.getVertex("leary_20th"), false);

        List<State> states = path.states;

        assertEquals(7, states.size());

        assertEquals("56th_24th", states.get(0).getVertex().getLabel());
        assertEquals("market_24th", states.get(1).getVertex().getLabel());
        assertEquals("market_ballard", states.get(2).getVertex().getLabel());
        assertEquals("market_22nd", states.get(3).getVertex().getLabel());
        assertEquals("market_leary", states.get(4).getVertex().getLabel());
        assertEquals("leary_vernon", states.get(5).getVertex().getLabel());
        assertEquals("leary_20th", states.get(6).getVertex().getLabel());
    }

    @Test
    public void testBack() {

        TraverseOptions options = new TraverseOptions();
        options.speed = 1.0;
        options.setArriveBy(true);

        ShortestPathTree tree = AStar.getShortestPathTree(_graph, "56th_24th", "leary_20th",
        		1000, options);

        GraphPath path = tree.getPath(_graph.getVertex("56th_24th"), false);

        List<State> states = path.states;

        assertTrue(states.size() == 6 || states.size() == 7);

        assertEquals("56th_24th", states.get(0).getVertex().getLabel());
        
        int n;
        //we could go either way around the block formed by 56th, 22nd, market, and 24th.
		if (states.size() == 7) {
        	assertEquals("market_24th", states.get(1).getVertex().getLabel());
        	assertEquals("market_ballard", states.get(2).getVertex().getLabel());
        	n = 0;
        } else {
        	assertEquals("56th_22nd", states.get(1).getVertex().getLabel());
        	n = -1;
        }
        
        assertEquals("market_22nd", states.get(n+3).getVertex().getLabel());
        assertEquals("market_leary", states.get(n+4).getVertex().getLabel());
        assertEquals("leary_vernon", states.get(n+5).getVertex().getLabel());
        assertEquals("leary_20th", states.get(n+6).getVertex().getLabel());
    }

    @Test
    public void testForwardExtraEdges() {

        TraverseOptions options = new TraverseOptions();
        options.speed = 1.0;

        StreetLocation fromLocation = new StreetLocation("near_shilshole_22nd", new Coordinate(
                -122.385050, 47.666620), "near_shilshole_22nd");
        fromLocation.getExtra().add(
                new SimpleEdge(fromLocation, _graph.getVertex("shilshole_22nd")));

        StreetLocation toLocation = new StreetLocation("near_56th_20th", new Coordinate(
                -122.382347, 47.669518), "near_56th_20th");
        toLocation.getExtra().add(new SimpleEdge(_graph.getVertex("56th_20th"), toLocation));

        ShortestPathTree tree = AStar.getShortestPathTree(_graph, fromLocation, toLocation,
        		0, options);

        GraphPath path = tree.getPath(toLocation, false);

        List<State> states = path.states;

        assertEquals(9, states.size());

        assertEquals("near_shilshole_22nd", states.get(0).getVertex().getLabel());
        assertEquals("shilshole_22nd", states.get(1).getVertex().getLabel());
        assertEquals("ballard_22nd", states.get(2).getVertex().getLabel());
        assertEquals("market_22nd", states.get(3).getVertex().getLabel());
        assertEquals("market_leary", states.get(4).getVertex().getLabel());
        assertEquals("market_russell", states.get(5).getVertex().getLabel());
        assertEquals("market_20th", states.get(6).getVertex().getLabel());
        assertEquals("56th_20th", states.get(7).getVertex().getLabel());
        assertEquals("near_56th_20th", states.get(8).getVertex().getLabel());
    }

    @Test
    public void testBackExtraEdges() {

        TraverseOptions options = new TraverseOptions();
        options.speed = 1.0;
        options.setArriveBy(true);

        StreetLocation fromLocation = new StreetLocation("near_shilshole_22nd", new Coordinate(
                -122.385050, 47.666620), "near_shilshole_22nd");
        fromLocation.getExtra().add(
                new SimpleEdge(fromLocation, _graph.getVertex("shilshole_22nd")));

        StreetLocation toLocation = new StreetLocation("near_56th_20th", new Coordinate(
                -122.382347, 47.669518), "near_56th_20th");
        toLocation.getExtra().add(new SimpleEdge(_graph.getVertex("56th_20th"), toLocation));

        ShortestPathTree tree = AStar.getShortestPathTree(_graph, fromLocation, toLocation,
        		1000, options);

        GraphPath path = tree.getPath(fromLocation, false);

        List<State> states = path.states;

        assertEquals(9, states.size());

        assertEquals("near_shilshole_22nd", states.get(0).getVertex().getLabel());
        assertEquals("shilshole_22nd", states.get(1).getVertex().getLabel());
        assertEquals("ballard_22nd", states.get(2).getVertex().getLabel());
        assertEquals("market_22nd", states.get(3).getVertex().getLabel());
        assertEquals("market_leary", states.get(4).getVertex().getLabel());
        assertEquals("market_russell", states.get(5).getVertex().getLabel());
        assertEquals("market_20th", states.get(6).getVertex().getLabel());
        assertEquals("56th_20th", states.get(7).getVertex().getLabel());
        assertEquals("near_56th_20th", states.get(8).getVertex().getLabel());
    }

    /****
     * Private Methods
     ****/

    private SimpleVertex vertex(String label, double lat, double lon) {
        SimpleVertex v = new SimpleVertex(_graph, label, lat, lon);
        return v;
    }

    private void edges(String... vLabels) {
        for (int i = 0; i < vLabels.length - 1; i++) {
            Vertex vA = _graph.getVertex(vLabels[i]);
            Vertex vB = _graph.getVertex(vLabels[i + 1]);

            new SimpleEdge(vA, vB);
            new SimpleEdge(vB, vA);
        }
    }

    private static class SimpleVertex extends AbstractVertex {

        private static final long serialVersionUID = 1L;

        public SimpleVertex(Graph g, String label, double lat, double lon) {
            super(g, label, lon, lat);
        }
    }

    private static class SimpleEdge extends AbstractEdge {
        private static final long serialVersionUID = 1L;

        public SimpleEdge(Vertex v1, Vertex v2) {
            super(v1, v2);
        }

        @Override
        public State traverse(State s0) {
            double d = getDistance();
            int t = (int) (d / s0.getOptions().speed);
            StateEditor s1 = s0.edit(this);
            s1.incrementTimeInSeconds(t);
            s1.incrementWeight(d);
            return s1.makeState();
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
        public Geometry getGeometry() {
            return null;
        }

        @Override
        public double getDistance() {
            return DistanceLibrary.distance(getFromVertex().getCoordinate(), getToVertex()
                    .getCoordinate());
        }
    }
}
