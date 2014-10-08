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

package org.opentripplanner.routing.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

import org.junit.Test;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

public class GraphServiceImplTest extends TestCase {

    @Test
    public final void testGraphServiceMemory() {

        GraphServiceImpl graphService = new GraphServiceImpl();
        Graph memoryA = new Graph();
        graphService.registerGraph("memA", new MemoryGraphSource("memA", memoryA));
        assertEquals(1, graphService.getRouterIds().size());

        Graph graph = graphService.getGraph("memA");
        assertNotNull(graph);
        assertEquals(memoryA, graph);
        assertEquals("memA", graph.routerId);

        try {
            graph = graphService.getGraph("inexistant");
            assertTrue(false); // Should not be there
        } catch (GraphNotFoundException e) {
        }

        graphService.setDefaultRouterId("memA");
        graph = graphService.getGraph();

        assertEquals(memoryA, graph);

        Graph memoryB = new Graph();
        graphService.registerGraph("memB", new MemoryGraphSource("memB", memoryB));
        assertEquals(2, graphService.getRouterIds().size());

        graph = graphService.getGraph("memB");
        assertNotNull(graph);
        assertEquals(memoryB, graph);
        assertEquals("memB", graph.routerId);

        graphService.evictGraph("memA");
        assertEquals(1, graphService.getRouterIds().size());

        try {
            graph = graphService.getGraph("memA");
            assertTrue(false); // Should not be there
        } catch (GraphNotFoundException e) {
        }
        try {
            graph = graphService.getGraph();
            assertTrue(false); // Should not be there
        } catch (GraphNotFoundException e) {
        }

        graphService.evictAll();
        assertEquals(0, graphService.getRouterIds().size());

    }

    @Test
    public final void testGraphServiceFile() throws IOException {

        // Ensure a dummy disk location exists
        File basePath = new File("test_graphs");
        if (!basePath.exists())
            basePath.mkdir();

        // Create a GraphService and a GraphSourceFactory
        GraphServiceImpl graphService = new GraphServiceImpl();
        FileGraphSourceFactory graphSourceFactory = new FileGraphSourceFactory();
        graphSourceFactory.basePath = basePath;

        // Create a dummy empty graph A and save it to disk
        Graph graphA = new Graph();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        graphA.save(new ObjectOutputStream(baos));
        byte[] graphData = baos.toByteArray();
        graphSourceFactory.save("A", new ByteArrayInputStream(graphData));

        // Check if the graph has been saved
        assertTrue(new File(new File(basePath, "A"), FileGraphSource.GRAPH_FILENAME).canRead());

        // Register this empty graph, reloading it from disk
        graphService.registerGraph("A", graphSourceFactory.createGraphSource("A"));

        // Check if the loaded graph is the one we saved earlier
        Graph graph = graphService.getGraph("A");
        assertNotNull(graph);
        assertEquals("A", graph.routerId);
        assertEquals(0, graph.getVertices().size());
        assertEquals(1, graphService.getRouterIds().size());

        // Add new data to our simple graph
        StreetVertex v1 = new IntersectionVertex(graphA, "v1", 0, 0);
        StreetVertex v2 = new IntersectionVertex(graphA, "v2", 0, 0.1);
        new StreetEdge(v1, v2, null, "v1v2", 11000, StreetTraversalPermission.PEDESTRIAN, false);
        int verticesCount = graphA.getVertices().size();
        int edgesCount = graphA.getEdges().size();

        // Serialize it again and save it again
        baos = new ByteArrayOutputStream();
        graphA.save(new ObjectOutputStream(baos));
        byte[] graphData2 = baos.toByteArray();
        graphSourceFactory.save("A", new ByteArrayInputStream(graphData2));

        // Force a reload, get again the graph
        graphService.reloadGraphs(false);
        graph = graphService.getGraph("A");

        // Check if loaded graph is the one modified
        assertEquals(verticesCount, graph.getVertices().size());
        assertEquals(edgesCount, graph.getEdges().size());

        // Evict the graph
        graphService.evictGraph("A");
        assertEquals(0, graphService.getRouterIds().size());
    }

}
