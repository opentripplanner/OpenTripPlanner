package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.SimpleVertex;
import org.opentripplanner.util.NonLocalizedString;

class PathwayEdgeTest {

    Graph graph = new Graph();
    Vertex from = new SimpleVertex(graph, "A", 10, 10);
    Vertex to = new SimpleVertex(graph, "B", 10.001, 10.001);

    @Test
    void zeroLength() {
        // if elevators have a traversal time and distance of 0 we cannot interpolate the distance
        // from the vertices as they most likely have identical coordinates
        var edge = new PathwayEdge(
                from,
                to,
                null,
                new NonLocalizedString("pathway"),
                0,
                0,
                0,
                0,
                true
        );

        assertThatEdgeIsTraversable(edge);
    }

    @Test
    void zeroLengthWithSteps() {
        var edge = new PathwayEdge(
                from,
                to,
                null,
                new NonLocalizedString("pathway"),
                0,
                0,
                2,
                0,
                true
        );

        assertThatEdgeIsTraversable(edge);
    }

    @Test
    void traversalTime() {
        var edge = new PathwayEdge(
                from,
                to,
                null,
                new NonLocalizedString("pathway"),
                60,
                0,
                0,
                0,
                true
        );

        var state = assertThatEdgeIsTraversable(edge);
        assertEquals(60, state.getElapsedTimeSeconds());
        assertEquals(120, state.getWeight());
    }

    @Test
    void traversalTimeOverridesLength() {
        var edge = new PathwayEdge(
                from,
                to,
                null,
                new NonLocalizedString("pathway"),
                60,
                1000,
                0,
                0,
                true
        );

        assertEquals(1000, edge.getDistanceMeters());

        var state = assertThatEdgeIsTraversable(edge);
        assertEquals(60, state.getElapsedTimeSeconds());
        assertEquals(120, state.getWeight());
    }

    @Test
    void distance() {
        var edge = new PathwayEdge(
                from,
                to,
                null,
                new NonLocalizedString("pathway"),
                0,
                100,
                0,
                0,
                true
        );

        var state = assertThatEdgeIsTraversable(edge);
        assertEquals(133, state.getElapsedTimeSeconds());
        assertEquals(266, state.getWeight());
    }

    private State assertThatEdgeIsTraversable(PathwayEdge edge) {
        var req = new RoutingRequest();
        req.setRoutingContext(graph, from, to);
        var state = new State(req);

        var afterTraversal = edge.traverse(state);
        assertNotNull(afterTraversal);

        assertTrue(afterTraversal.getWeight() > 0);
        return afterTraversal;
    }

}