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

import junit.framework.TestCase;

import org.junit.Test;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph;

public class GraphServiceImplTest extends TestCase {

    @Test
    public final void testGraphService() {

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
}
