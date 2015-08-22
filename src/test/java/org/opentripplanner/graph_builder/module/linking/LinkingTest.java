package org.opentripplanner.graph_builder.module.linking;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import jersey.repackaged.com.google.common.collect.Iterables;
import jersey.repackaged.com.google.common.collect.Maps;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.profile.StopTreeCache;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.*;
import static org.opentripplanner.graph_builder.module.FakeGraph.*;

public class LinkingTest {
    /** maximum difference in walk distance, in meters, that is acceptable between the graphs */
    public static final int EPSILON = 1;

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

            P2<StreetEdge> sp0 = s0.split(sv0);
            P2<StreetEdge> sp1 = s1.split(sv1);

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
        for (TransitStop ts : Iterables.filter(g1.getVertices(), TransitStop.class)) {
            Collection<Edge> stls = stls(ts.getOutgoing());			
            assertTrue(stls.size() >= 1);

            StreetTransitLink exemplar = (StreetTransitLink) stls.iterator().next();

            TransitStop other = (TransitStop) g2.getVertex(ts.getLabel());

            Collection<Edge> ostls = stls(other.getOutgoing());

            assertEquals("Unequal number of links from stop " + ts, stls.size(), ostls.size());

            StreetTransitLink oe = (StreetTransitLink) ostls.iterator().next();

            assertEquals(exemplar.getToVertex().getLat(), oe.getToVertex().getLat(), 1e-10);
            assertEquals(exemplar.getToVertex().getLon(), oe.getToVertex().getLon(), 1e-10);
        }

        // compare the stop tree caches
        g1.index(new DefaultStreetVertexIndexFactory());
        g2.index(new DefaultStreetVertexIndexFactory());

        g1.rebuildVertexAndEdgeIndices();
        g2.rebuildVertexAndEdgeIndices();

        StopTreeCache s1 = g1.index.getStopTreeCache();
        StopTreeCache s2 = g2.index.getStopTreeCache();

        // convert the caches to be by stop label
        Map<String, int[]> l1 = cacheByLabel(s1);
        Map<String, int[]> l2 = cacheByLabel(s2);

        // do the comparison
        for (Entry<String, int[]> e : l1.entrySet()) {
            // graph 2 should contain all stops in graph 1 (and a few more)
            assertTrue(l2.containsKey(e.getKey()));

            TObjectIntMap<String> g1t = jaggedArrayToVertexMap(e.getValue(), g1);
            TObjectIntMap<String> g2t = jaggedArrayToVertexMap(l2.get(e.getKey()), g2);

            for (TObjectIntIterator<String> it = g1t.iterator(); it.hasNext();) {
                it.advance();

                assertTrue(g2t.containsKey(it.key()));

                int newv = g2t.get(it.key());

                assertTrue("At " + it.key() + " from stop " + g1.getVertex(e.getKey()) + ", difference in walk distances: " + it.value() + "m without extra stops,"  + newv + "m with",
                        Math.abs(it.value() - newv) <= EPSILON);
            }
        }
    }

    private TObjectIntMap<String> jaggedArrayToVertexMap(int[] value, Graph g) {
        TObjectIntMap<String> ret = new TObjectIntHashMap<String>();

        for (int i = 0; i < value.length; i++) {
            Vertex v = g.getVertexById(value[i++]);

            if (!v.getLabel().startsWith("osm:node"))
                continue;

            ret.put(v.getLabel(), value[i]);
        }

        return ret;
    }

    private static Collection<Edge> stls (Collection<Edge> edges) {
        return Collections2.filter(edges, new Predicate<Edge>() {

            @Override
            public boolean apply(Edge input) {
                return input instanceof StreetTransitLink;
            }
        });
    }

    /** get the stop tree cache indexed by label */
    public static Map<String, int[]> cacheByLabel (StopTreeCache c) {
        Map<String, int[]> ret = Maps.newHashMap();

        for (Entry<TransitStop, int[]> e : c.distancesForStop.entrySet()) {
            ret.put(e.getKey().getLabel(), e.getValue());
        }

        return ret;
    }
}
