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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import junit.framework.TestCase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.opentripplanner.graph_builder.module.EmbedConfig;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

public class GraphServiceTest extends TestCase {

    File basePath;

    Graph emptyGraph;

    Graph smallGraph;

    byte[] emptyGraphData;

    byte[] smallGraphData;

    @Override
    protected void setUp() throws IOException {
        // Ensure a dummy disk location exists
        basePath = new File("test_graphs");
        if (!basePath.exists())
            basePath.mkdir();

        // Create an empty graph and it's serialized form
        emptyGraph = new Graph();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        emptyGraph.save(new ObjectOutputStream(baos));
        emptyGraphData = baos.toByteArray();

        // Create a small graph with 2 vertices and one edge and it's serialized form
        smallGraph = new Graph();
        StreetVertex v1 = new IntersectionVertex(smallGraph, "v1", 0, 0);
        StreetVertex v2 = new IntersectionVertex(smallGraph, "v2", 0, 0.1);
        new StreetEdge(v1, v2, null, "v1v2", 11000, StreetTraversalPermission.PEDESTRIAN, false);
        baos = new ByteArrayOutputStream();
        smallGraph.save(new ObjectOutputStream(baos));
        smallGraphData = baos.toByteArray();
    }

    @Override
    protected void tearDown() throws FileNotFoundException {
        deleteRecursive(basePath);
        basePath = null;
    }

    public static boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists())
            throw new FileNotFoundException(path.getAbsolutePath());
        boolean ret = true;
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }

    @Test
    public final void testGraphServiceMemory() {

        GraphService graphService = new GraphService();
        graphService.registerGraph("A", new MemoryGraphSource("A", emptyGraph));
        assertEquals(1, graphService.getRouterIds().size());

        Graph graph = graphService.getRouter("A").graph;
        assertNotNull(graph);
        assertEquals(emptyGraph, graph);
        assertEquals("A", emptyGraph.routerId);

        try {
            graph = graphService.getRouter("inexistant").graph;
            assertTrue(false); // Should not be there
        } catch (GraphNotFoundException e) {
        }

        graphService.setDefaultRouterId("A");
        graph = graphService.getRouter().graph;

        assertEquals(emptyGraph, graph);

        graphService.registerGraph("B", new MemoryGraphSource("B", smallGraph));
        assertEquals(2, graphService.getRouterIds().size());

        graph = graphService.getRouter("B").graph;
        assertNotNull(graph);
        assertEquals(smallGraph, graph);
        assertEquals("B", graph.routerId);

        graphService.evictRouter("A");
        assertEquals(1, graphService.getRouterIds().size());

        try {
            graph = graphService.getRouter("A").graph;
            assertTrue(false); // Should not be there
        } catch (GraphNotFoundException e) {
        }
        try {
            graph = graphService.getRouter().graph;
            assertTrue(false); // Should not be there
        } catch (GraphNotFoundException e) {
        }

        graphService.evictAll();
        assertEquals(0, graphService.getRouterIds().size());

    }

    @Test
    public final void testGraphServiceFile() throws IOException {

        // Create a GraphService and a GraphSourceFactory
        GraphService graphService = new GraphService();
        InputStreamGraphSource.FileFactory graphSourceFactory = new InputStreamGraphSource.FileFactory(basePath);

        graphSourceFactory.save("A", new ByteArrayInputStream(emptyGraphData));

        // Check if the graph has been saved
        assertTrue(new File(new File(basePath, "A"), InputStreamGraphSource.GRAPH_FILENAME).canRead());

        // Register this empty graph, reloading it from disk
        boolean registered = graphService.registerGraph("A",
                graphSourceFactory.createGraphSource("A"));
        assertTrue(registered);

        // Check if the loaded graph is the one we saved earlier
        Graph graph = graphService.getRouter("A").graph;
        assertNotNull(graph);
        assertEquals("A", graph.routerId);
        assertEquals(0, graph.getVertices().size());
        assertEquals(1, graphService.getRouterIds().size());

        // Save A again, with more data this time
        int verticesCount = smallGraph.getVertices().size();
        int edgesCount = smallGraph.getEdges().size();
        graphSourceFactory.save("A", new ByteArrayInputStream(smallGraphData));

        // Force a reload, get again the graph
        graphService.reloadGraphs(false, true);
        graph = graphService.getRouter("A").graph;

        // Check if loaded graph is the one modified
        assertEquals(verticesCount, graph.getVertices().size());
        assertEquals(edgesCount, graph.getEdges().size());

        // Remove the file from disk and reload
        boolean deleted = new File(new File(basePath, "A"), InputStreamGraphSource.GRAPH_FILENAME)
                .delete();
        assertTrue(deleted);
        graphService.reloadGraphs(false, true);

        // Check that the graph have been evicted
        assertEquals(0, graphService.getRouterIds().size());

        // Register it manually
        registered = graphService.registerGraph("A", graphSourceFactory.createGraphSource("A"));
        // Check that it fails again (file still deleted)
        assertFalse(registered);
        assertEquals(0, graphService.getRouterIds().size());

        // Re-save the graph file, register it again
        graphSourceFactory.save("A", new ByteArrayInputStream(smallGraphData));
        registered = graphService.registerGraph("A", graphSourceFactory.createGraphSource("A"));
        // This time registering is OK
        assertTrue(registered);
        assertEquals(1, graphService.getRouterIds().size());

        // Evict the graph, should be OK
        boolean evicted = graphService.evictRouter("A");
        assertTrue(evicted);
        assertEquals(0, graphService.getRouterIds().size());
    }

    @Test
    public final void testGraphServiceAutoscan() throws IOException {

        // Check for no graphs
        GraphService graphService = new GraphService(false);
        GraphScanner graphScanner = new GraphScanner(graphService, basePath, true);
        graphScanner.startup();
        assertEquals(0, graphService.getRouterIds().size());

        System.out.println("------------------------------------------");
        // Add a single default graph
        InputStreamGraphSource.FileFactory graphSourceFactory = new InputStreamGraphSource.FileFactory(basePath);
        graphSourceFactory.save("", new ByteArrayInputStream(smallGraphData));

        // Check that the single graph is there
        graphService = new GraphService(false);
        graphScanner = new GraphScanner(graphService, basePath, true);
        graphScanner.startup();
        assertEquals(1, graphService.getRouterIds().size());
        assertEquals("", graphService.getRouter().graph.routerId);
        assertEquals("", graphService.getRouter("").graph.routerId);

        System.out.println("------------------------------------------");
        // Add another graph in a sub-directory
        graphSourceFactory.save("A", new ByteArrayInputStream(smallGraphData));
        graphService = new GraphService(false);
        graphScanner = new GraphScanner(graphService, basePath, true);
        graphScanner.startup();
        assertEquals(2, graphService.getRouterIds().size());
        assertEquals("", graphService.getRouter().graph.routerId);
        assertEquals("A", graphService.getRouter("A").graph.routerId);

        System.out.println("------------------------------------------");
        // Remove default Graph
        new File(basePath, InputStreamGraphSource.GRAPH_FILENAME).delete();

        // Check that default is A this time
        graphService = new GraphService(false);
        graphScanner = new GraphScanner(graphService, basePath, true);
        graphScanner.startup();
        assertEquals(1, graphService.getRouterIds().size());
        assertEquals("A", graphService.getRouter().graph.routerId);
        assertEquals("A", graphService.getRouter("A").graph.routerId);

    }

    @Test
    public final void testGraphServiceMemoryRouterConfig () throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode config = mapper.createObjectNode();
        config.put("timeout", 8);
        EmbedConfig embedConfig = new EmbedConfig(null, config);
        embedConfig.buildGraph(emptyGraph, null);

        GraphService graphService = new GraphService();
        graphService.registerGraph("A", new MemoryGraphSource("A", emptyGraph));
        assertEquals(1, graphService.getRouterIds().size());

        Graph graph = graphService.getRouter("A").graph;
        assertNotNull(graph);
        assertEquals(emptyGraph, graph);
        assertEquals("A", emptyGraph.routerId);

        JsonNode routerConfig = mapper.readTree(graph.routerConfig);
        assertEquals(routerConfig, config);
        assertEquals(routerConfig.get("timeout"), config.get("timeout"));
    }
}
