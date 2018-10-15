package org.opentripplanner.routing.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RoutingContext_Destroy_Test {

    private static final boolean ARRIVE_BY_TRUE = true;

    private static final boolean ARRIVE_BY_FALSE = false;

    private final GeometryFactory gf = GeometryUtils.getGeometryFactory();
    private RoutingRequest request;
    private RoutingContext subject;

    // Given:
    // - a graph with 3 intersections/vertexes
    private final Graph g = new Graph();

    private final StreetVertex a = new IntersectionVertex(g, "A", 1.0, 1.0);

    private final StreetVertex b = new IntersectionVertex(g, "B", 0.0, 1.0);

    private final StreetVertex c = new IntersectionVertex(g, "C", 1.0, 0.0);

    private final List<Vertex> permanentVertexes = Arrays.asList(a, b, c);

    // - And travel *origin* is 0,4 degrees on the road from B to A
    private final GenericLocation from = new GenericLocation(1.0, 0.4);

    // - and *destination* is slightly off 0.7 degrees on road from C to A
    private final GenericLocation to = new GenericLocation(0.701, 1.001);

    // - and some roads
    @Before public void setup() {
        createStreetEdge(a, b, "a -> b");
        createStreetEdge(b, a, "b -> a");
        createStreetEdge(a, c, "a -> c");
        g.index(new DefaultStreetVertexIndexFactory());
    }

    @Test public void temporary_changes_is_removed_from_graph_on_context_destroy() {
        // try arriveBy set to both true and false
        temporary_changes_is_removed_from_graph_on_context_destroy(ARRIVE_BY_TRUE);
        temporary_changes_is_removed_from_graph_on_context_destroy(ARRIVE_BY_FALSE);
    }

    private void temporary_changes_is_removed_from_graph_on_context_destroy(boolean arriveBy) {
        // Given - A request
        request = new RoutingRequest();
        request.from = from;
        request.to = to;
        request.arriveBy = arriveBy;

        // When - the context is created
        subject = new RoutingContext(request, g);

        // Then:
        origin_and_destination_is_inserted_correct_in_the_graph();

        // And When:
        subject.destroy();

        // Then - permanent vertexes
        for (Vertex v : permanentVertexes) {
            // - does not reference the any temporary nodes any more
            for (Edge e : v.getIncoming()) {
                assertVertexEdgeIsNotReferencingTemporaryElements(v, e, e.getFromVertex());
            }
            for (Edge e : v.getOutgoing()) {
                assertVertexEdgeIsNotReferencingTemporaryElements(v, e, e.getToVertex());
            }
        }
    }

    private void origin_and_destination_is_inserted_correct_in_the_graph() {
        // Then - the origin and destination is
        assertEquals("Origin", subject.fromVertex.getName());
        assertEquals("Destination", subject.toVertex.getName());

        // And - from the origin
        Collection<String> vertexesReachableFromOrigin = findAllReachableVertexes(
                subject.fromVertex, true, new ArrayList<>());
        String msg = "All reachable vertexes from origin: " + vertexesReachableFromOrigin;

        // it is possible to reach the A, B, C and the Destination Vertex
        assertTrue(msg, vertexesReachableFromOrigin.contains("A"));
        assertTrue(msg, vertexesReachableFromOrigin.contains("B"));
        assertTrue(msg, vertexesReachableFromOrigin.contains("C"));
        assertTrue(msg, vertexesReachableFromOrigin.contains("Destination"));

        // And - from the destination we can backtrack
        Collection<String> vertexesReachableFromDestination = findAllReachableVertexes(
                subject.toVertex, false, new ArrayList<>());
        msg = "All reachable vertexes back from destination: " + vertexesReachableFromDestination;

        // and reach the A, B and the Origin Vertex
        assertTrue(msg, vertexesReachableFromDestination.contains("A"));
        assertTrue(msg, vertexesReachableFromDestination.contains("B"));
        assertTrue(msg, vertexesReachableFromDestination.contains("Origin"));

        // But - not the C Vertex
        assertFalse(msg, vertexesReachableFromDestination.contains("C"));
    }

    private void createStreetEdge(StreetVertex v0, StreetVertex v1, String name) {
        LineString geom = gf
                .createLineString(new Coordinate[] { v0.getCoordinate(), v1.getCoordinate() });
        double dist = SphericalDistanceLibrary.distance(v0.getCoordinate(), v1.getCoordinate());
        new StreetEdge(v0, v1, geom, name, dist, StreetTraversalPermission.ALL, false);
    }

    private static <T extends Collection<String>> T findAllReachableVertexes(Vertex vertex,
            boolean forward, T list) {
        if (list.contains(vertex.getName()))
            return list;

        list.add(vertex.getName());
        if (forward) {
            vertex.getOutgoing()
                    .forEach(it -> findAllReachableVertexes(it.getToVertex(), forward, list));
        } else {
            vertex.getIncoming()
                    .forEach(it -> findAllReachableVertexes(it.getFromVertex(), forward, list));
        }
        return list;
    }

    private void assertVertexEdgeIsNotReferencingTemporaryElements(Vertex source, Edge e,
            Vertex v) {
        String sourceName = source.getName();
        assertFalse(sourceName + " -> " + e.getName(), e instanceof TemporaryEdge);
        assertFalse(sourceName + " -> " + v.getName(), v instanceof TemporaryVertex);
    }
}
