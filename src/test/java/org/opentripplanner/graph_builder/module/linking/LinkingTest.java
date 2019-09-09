package org.opentripplanner.graph_builder.module.linking;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.StopVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.io.UnsupportedEncodingException;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.opentripplanner.graph_builder.module.FakeGraph.*;

public class LinkingTest {

    /**
     * Ensure that splitting edges yields edges that are identical in length for forward and back edges.
     * StreetEdges have lengths expressed internally in mm, and we want to be sure that not only do they
     * sum to the same values but also that they 
     */
    @Test
    public void testSplitting () {
        GeometryFactory gf= GeometryUtils.getGeometryFactory();
        double x = -122.123;
        double y = 37.363; 
        for (double delta = 0; delta <= 2; delta += 0.005) {

            StreetVertex v0 = new IntersectionVertex(null, "zero", x, y);
            StreetVertex v1 = new IntersectionVertex(null, "one", x + delta, y + delta);
            LineString geom = gf.createLineString(new Coordinate[] { v0.getCoordinate(), v1.getCoordinate() });
            double dist = SphericalDistanceLibrary.distance(v0.getCoordinate(), v1.getCoordinate());
            StreetEdge s0 = new StreetEdge(v0, v1, geom, "test", dist, StreetTraversalPermission.ALL, false);
            StreetEdge s1 = new StreetEdge(v1, v0, (LineString) geom.reverse(), "back", dist, StreetTraversalPermission.ALL, true);

            // split it but not too close to the end
            double splitVal = Math.random() * 0.95 + 0.025;

            SplitterVertex sv0 = new SplitterVertex(null, "split", x + delta * splitVal, y + delta * splitVal, s0);
            SplitterVertex sv1 = new SplitterVertex(null, "split", x + delta * splitVal, y + delta * splitVal, s1);

            P2<StreetEdge> sp0 = s0.split(sv0, true);
            P2<StreetEdge> sp1 = s1.split(sv1, true);

            // distances expressed internally in mm so this epsilon is plenty good enough to ensure that they
            // have the same values
            assertEquals(sp0.first.getDistance(), sp1.second.getDistance(), 0.0000001);
            assertEquals(sp0.second.getDistance(), sp1.first.getDistance(), 0.0000001);
            assertFalse(sp0.first.isBack());
            assertFalse(sp0.second.isBack());
            assertTrue(sp1.first.isBack());
            assertTrue(sp1.second.isBack());
        }
    }

    /**
     * Test that all the stops are linked identically
     * to the street network on two builds of similar graphs
     * with additional stops in one.
     * 
     * We do this by building the graphs and then comparing the stop tree caches.
     */
    @Test
    public void testStopsLinkedIdentically () throws UnsupportedEncodingException {
        // build the graph without the added stops
        Graph g1 = buildGraphNoTransit();
        addRegularStopGrid(g1);
        link(g1);

        Graph g2 = buildGraphNoTransit();
        addExtraStops(g2);
        addRegularStopGrid(g2);
        link(g2);

        // compare the linkages
        for (StopVertex ts : Iterables.filter(g1.getVertices(), StopVertex.class)) {
            Collection<Edge> stls = stls(ts.getOutgoing());			
            assertTrue(stls.size() >= 1);

            StreetTransitLink exemplar = (StreetTransitLink) stls.iterator().next();

            StopVertex other = (StopVertex) g2.getVertex(ts.getLabel());

            Collection<Edge> ostls = stls(other.getOutgoing());

            assertEquals("Unequal number of links from stop " + ts, stls.size(), ostls.size());

            StreetTransitLink oe = (StreetTransitLink) ostls.iterator().next();

            assertEquals(exemplar.getToVertex().getLat(), oe.getToVertex().getLat(), 1e-10);
            assertEquals(exemplar.getToVertex().getLon(), oe.getToVertex().getLon(), 1e-10);
        }
    }

    private static Collection<Edge> stls (Collection<Edge> edges) {
        return Collections2.filter(edges, new Predicate<Edge>() {

            @Override
            public boolean apply(Edge input) {
                return input instanceof StreetTransitLink;
            }
        });
    }

}
