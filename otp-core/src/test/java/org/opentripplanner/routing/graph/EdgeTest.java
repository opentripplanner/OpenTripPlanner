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

package org.opentripplanner.routing.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EdgeTest {
    @Test
    public void testConstruct() {
        Graph graph = new Graph();
        Vertex head = new SimpleConcreteVertex(
                graph, "head", 47.669457, -122.387577);
        Vertex tail = new SimpleConcreteVertex(
                graph, "tail", 47.669462, -122.384739);
        Edge e = new SimpleConcreteEdge(head, tail);

        assertEquals(head, e.getFromVertex());
        assertEquals(tail, e.getToVertex());
        assertTrue(e.getId() >= 0);
    }
}
