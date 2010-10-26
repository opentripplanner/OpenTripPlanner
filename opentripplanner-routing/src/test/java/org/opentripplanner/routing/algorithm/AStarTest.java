package org.opentripplanner.routing.algorithm;

import static org.junit.Assert.assertEquals;

import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTVertex;
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
                new State(), options);

        GraphPath path = tree.getPath(_graph.getVertex("leary_20th"));

        Vector<SPTVertex> vertices = path.vertices;

        assertEquals(7, vertices.size());

        assertEquals("56th_24th", vertices.get(0).getLabel());
        assertEquals("market_24th", vertices.get(1).getLabel());
        assertEquals("market_ballard", vertices.get(2).getLabel());
        assertEquals("market_22nd", vertices.get(3).getLabel());
        assertEquals("market_leary", vertices.get(4).getLabel());
        assertEquals("leary_vernon", vertices.get(5).getLabel());
        assertEquals("leary_20th", vertices.get(6).getLabel());
    }

    @Test
    public void testBack() {

        TraverseOptions options = new TraverseOptions();
        options.speed = 1.0;
        options.setArriveBy(true);

        ShortestPathTree tree = AStar.getShortestPathTreeBack(_graph, "56th_24th", "leary_20th",
                new State(), options);

        GraphPath path = tree.getPath(_graph.getVertex("56th_24th"));

        Vector<SPTVertex> vertices = path.vertices;

        assertEquals(7, vertices.size());

        assertEquals("leary_20th", vertices.get(0).getLabel());
        assertEquals("leary_vernon", vertices.get(1).getLabel());
        assertEquals("market_leary", vertices.get(2).getLabel());
        assertEquals("market_22nd", vertices.get(3).getLabel());
        assertEquals("market_ballard", vertices.get(4).getLabel());
        assertEquals("market_24th", vertices.get(5).getLabel());
        assertEquals("56th_24th", vertices.get(6).getLabel());
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
                new State(), options);

        GraphPath path = tree.getPath(toLocation);

        Vector<SPTVertex> vertices = path.vertices;

        assertEquals(9, vertices.size());

        assertEquals("near_shilshole_22nd", vertices.get(0).getLabel());
        assertEquals("shilshole_22nd", vertices.get(1).getLabel());
        assertEquals("ballard_22nd", vertices.get(2).getLabel());
        assertEquals("market_22nd", vertices.get(3).getLabel());
        assertEquals("market_leary", vertices.get(4).getLabel());
        assertEquals("market_russell", vertices.get(5).getLabel());
        assertEquals("market_20th", vertices.get(6).getLabel());
        assertEquals("56th_20th", vertices.get(7).getLabel());
        assertEquals("near_56th_20th", vertices.get(8).getLabel());
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

        ShortestPathTree tree = AStar.getShortestPathTreeBack(_graph, fromLocation, toLocation,
                new State(), options);

        GraphPath path = tree.getPath(fromLocation);

        Vector<SPTVertex> vertices = path.vertices;

        assertEquals(9, vertices.size());

        assertEquals("near_56th_20th", vertices.get(0).getLabel());
        assertEquals("56th_20th", vertices.get(1).getLabel());
        assertEquals("market_20th", vertices.get(2).getLabel());
        assertEquals("market_russell", vertices.get(3).getLabel());
        assertEquals("market_leary", vertices.get(4).getLabel());
        assertEquals("market_22nd", vertices.get(5).getLabel());
        assertEquals("ballard_22nd", vertices.get(6).getLabel());
        assertEquals("shilshole_22nd", vertices.get(7).getLabel());
        assertEquals("near_shilshole_22nd", vertices.get(8).getLabel());
    }

    /****
     * Private Methods
     ****/

    private SimpleVertex vertex(String label, double lat, double lon) {
        SimpleVertex v = new SimpleVertex(label, lat, lon);
        _graph.addVertex(v);
        return v;
    }

    private void edges(String... vLabels) {
        for (int i = 0; i < vLabels.length - 1; i++) {
            Vertex vA = _graph.getVertex(vLabels[i]);
            Vertex vB = _graph.getVertex(vLabels[i + 1]);

            _graph.addEdge(vA, vB, new SimpleEdge(vA, vB));
            _graph.addEdge(vB, vA, new SimpleEdge(vB, vA));
        }
    }

    private static class SimpleVertex extends GenericVertex {

        private static final long serialVersionUID = 1L;

        public SimpleVertex(String label, double lat, double lon) {
            super(label, lon, lat);
        }
    }

    private static class SimpleEdge extends AbstractEdge {
        private static final long serialVersionUID = 1L;

        public SimpleEdge(Vertex v1, Vertex v2) {
            super(v1, v2);
        }

        @Override
        public TraverseResult traverse(State s0, TraverseOptions options)
                throws NegativeWeightException {
            double d = getDistance();
            long t = (long) (d / options.speed);
            return new TraverseResult(t, new State(s0.getTime() + t));
        }

        @Override
        public TraverseResult traverseBack(State s0, TraverseOptions options)
                throws NegativeWeightException {
            double d = getDistance();
            long t = (long) (d / options.speed);
            return new TraverseResult(t, new State(s0.getTime() - t));
        }

        @Override
        public TraverseMode getMode() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getDirection() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Geometry getGeometry() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public double getDistance() {
            return DistanceLibrary.distance(getFromVertex().getCoordinate(), getToVertex()
                    .getCoordinate());
        }
    }
}
