package org.opentripplanner.graph_builder.module.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.vertextype.StreetVertex;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import java.util.Locale;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

public class TestStreetMatcher {
    static GeometryFactory gf = new GeometryFactory();

    private Graph graph;

    @Before
    public void before() {

        graph = new Graph();

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
    public void testStreetMatcher() {
        
        LineString geometry = geometry(-122.385689, 47.669484, -122.387384, 47.669470);

        StreetMatcher matcher = new StreetMatcher(graph);

        List<Edge> match = matcher.match(geometry);
        assertNotNull(match);
        assertEquals(1, match.size());
        assertEquals("56th_24th", match.get(0).getToVertex().getLabel());

        geometry = geometry(-122.385689, 47.669484, -122.387384, 47.669470, -122.387588, 47.669325);

        match = matcher.match(geometry);
        assertNotNull(match);
        assertEquals(2, match.size());

        geometry = geometry(-122.385689, 47.669484, -122.387384, 47.669470, -122.387588, 47.669325,
                -122.387255, 47.668675);

        match = matcher.match(geometry);
        assertNotNull(match);
        assertEquals(3, match.size());

        geometry = geometry(-122.384756, 47.669260, -122.384777, 47.667454, -122.383554, 47.666789,
                -122.3825, 47.666);
         match = matcher.match(geometry);
        assertNotNull(match);
        System.out.println(match);
        assertEquals(4, match.size());
        assertEquals("ballard_20th", match.get(3).getToVertex().getLabel());
    }

    private LineString geometry(double... ordinates) {
        Coordinate[] coords = new Coordinate[ordinates.length / 2];

        for (int i = 0; i < ordinates.length; i += 2) {
            coords[i / 2] = new Coordinate(ordinates[i], ordinates[i + 1]);
        }
        return gf.createLineString(coords);
    }

    /****
     * Private Methods
     ****/

    private SimpleVertex vertex(String label, double lat, double lon) {
        SimpleVertex v = new SimpleVertex(graph, label, lat, lon);
        return v;
    }

    private void edges(String... vLabels) {
        for (int i = 0; i < vLabels.length - 1; i++) {
            StreetVertex vA = (StreetVertex) graph.getVertex(vLabels[i]);
            StreetVertex vB = (StreetVertex) graph.getVertex(vLabels[i + 1]);

            new SimpleEdge(vA, vB);
            new SimpleEdge(vB, vA);
        }
    }

    private static class SimpleVertex extends StreetVertex {

        private static final long serialVersionUID = 1L;

        public SimpleVertex(Graph g, String label, double lat, double lon) {
            super(g, label, lon, lat, new NonLocalizedString(label));
        }
    }

    /* TODO explain why this exists and is "simple" */
    private static class SimpleEdge extends StreetEdge {
        private static final long serialVersionUID = 1L;

        public SimpleEdge(StreetVertex v1, StreetVertex v2) {
            super(v1, v2, null, (NonLocalizedString)null, 0, null, false);
        }
        
        @Override
        public State traverse(State s0) {
            double d = getDistanceMeters();
            TraverseMode mode = s0.getNonTransitMode();
            int t = (int) (d / s0.getOptions().getSpeed(mode, false));
            StateEditor s1 = s0.edit(this);
            s1.incrementTimeInSeconds(t);
            s1.incrementWeight(d);
            return s1.makeState();
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public I18NString getRawName() {
            return null;
        }

        @Override
        public String getName(Locale locale) {
            return null;
        }

        @Override
        public LineString getGeometry() {
            return gf.createLineString(new Coordinate[] { fromv.getCoordinate(),
                    tov.getCoordinate() });
        }

        @Override
        public double getDistanceMeters() {
            return SphericalDistanceLibrary.distance(getFromVertex().getCoordinate(), getToVertex().getCoordinate());
        }

        @Override
        public PackedCoordinateSequence getElevationProfile() {
            return null;
        }

        @Override
        public boolean canTraverse(TraverseModeSet modes) {
            return true;
        }
        
        @Override
        public StreetTraversalPermission getPermission() {
            return StreetTraversalPermission.ALL;
        }

        @Override
        public boolean isMotorVehicleNoThruTraffic() {
            return false;
        }

        public String toString() {
            return "SimpleEdge(" + fromv + ", " + tov + ")";
        }
        
        @Override
        public int getStreetClass() {
            return StreetEdge.CLASS_STREET;
        }

        @Override
        public boolean isWheelchairAccessible() {
            return true;
        }

        public boolean isElevationFlattened() {
            return false;
        }

        @Override
        public float getCarSpeed() {
            return 11.2f;
        }

        @Override
        public int getInAngle() {
            return 0;
        }

        @Override
        public int getOutAngle() {
            return 0;
        }

        @Override
        public void setCarSpeed(float carSpeed) {}

    }
}
