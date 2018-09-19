package org.opentripplanner.serializer;

import org.junit.Test;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for the GraphSerializerService.
 * For testing a specific implementation of {@link GraphSerializer} thoroughly, this is not the right place.
 */
public class GraphSerializerServiceTest {

    /**
     * Simple verification that the java serializer implementation is returned when asking for it.
     */
    @Test
    public void getJavaImplementation() {
        GraphSerializerService graphSerializerService = new GraphSerializerService(GraphSerializerService.SerializationMethod.JAVA);
        assertEquals(JavaGraphSerializer.class, graphSerializerService.getGraphSerializer().getClass());
    }

    /**
     * Save and load a simple graph and do some basic checks.
     */
    @Test
    public void saveAndLoadGraphUsingTheDefaultSerializer() {

        GraphSerializerService graphSerializerService = new GraphSerializerService();

        Graph graph = createSimpleGraph();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        graphSerializerService.save(graph, byteArrayOutputStream);

        byte[] bytes = byteArrayOutputStream.toByteArray();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);

        Graph actual = graphSerializerService.load(byteArrayInputStream);

        assertNotNull(actual);
        assertEquals(graph.getEdges().size(), actual.getEdges().size());
        assertEquals(graph.getVertices().size(), actual.getVertices().size());
    }

    private Graph createSimpleGraph() {
        // Create a small graph with 2 vertices and one edge and it's serialized form
        Graph smallGraph = new Graph();
        StreetVertex v1 = new IntersectionVertex(smallGraph, "v1", 0, 0);
        StreetVertex v2 = new IntersectionVertex(smallGraph, "v2", 0, 0.1);
        new StreetEdge(v1, v2, null, "v1v2", 11000, StreetTraversalPermission.PEDESTRIAN, false);

        return smallGraph;
    }
}