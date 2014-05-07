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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.util.ArrayUtils.contains;

import org.junit.Test;
import org.opentripplanner.routing.alertpatch.AlertPatch;

public class GraphTest {
    @Test
    public final void testAlertPatch() {
        final AlertPatch alertPatches[][] = new AlertPatch[10][];
        Graph graph = new Graph();
        Vertex vertex0 = new SimpleConcreteVertex(graph, "Vertex 0", 0, 0);
        Vertex vertex1 = new SimpleConcreteVertex(graph, "Vertex 1", 0, 180);
        Edge edge0 = new SimpleConcreteEdge(vertex0, vertex1);
        Edge edge1 = new SimpleConcreteEdge(vertex1, vertex0);
        AlertPatch alertPatch0 = new AlertPatch();
        AlertPatch alertPatch1 = new AlertPatch();
        AlertPatch alertPatch2 = new AlertPatch();
        AlertPatch alertPatch3 = new AlertPatch();

        alertPatch0.setId("A");
        alertPatch1.setId("A");
        alertPatch2.setId("B");
        alertPatch3.setId("C");

        graph.addAlertPatch(null, null);
        graph.addAlertPatch(null, alertPatch0);
        graph.addAlertPatch(null, alertPatch3);
        graph.addAlertPatch(edge0, null);
        graph.removeAlertPatch(edge1, null);
        graph.removeAlertPatch(null, alertPatch1);
        graph.removeAlertPatch(null, alertPatch2);
        graph.removeAlertPatch(null, null);
        alertPatches[0] = graph.getAlertPatches(null);
        alertPatches[1] = graph.getAlertPatches(edge0);
        graph.addAlertPatch(edge0, alertPatch0);
        alertPatches[2] = graph.getAlertPatches(edge0);
        graph.addAlertPatch(edge0, alertPatch1);
        alertPatches[3] = graph.getAlertPatches(edge1);
        graph.addAlertPatch(edge1, alertPatch3);
        alertPatches[4] = graph.getAlertPatches(edge0);
        graph.removeAlertPatch(edge0, alertPatch1);
        alertPatches[5] = graph.getAlertPatches(edge0);
        graph.addAlertPatch(edge1, alertPatch2);
        alertPatches[6] = graph.getAlertPatches(edge1);
        graph.removeAlertPatch(edge1, alertPatch2);
        graph.removeAlertPatch(edge1, alertPatch2);
        alertPatches[7] = graph.getAlertPatches(edge1);
        graph.addAlertPatch(edge0, alertPatch0);
        graph.addAlertPatch(edge0, alertPatch1);
        graph.addAlertPatch(edge0, alertPatch2);
        graph.addAlertPatch(edge0, alertPatch3);
        alertPatches[8] = graph.getAlertPatches(edge0);
        graph.removeAlertPatch(edge0, alertPatch2);
        graph.removeAlertPatch(edge0, alertPatch1);
        graph.removeAlertPatch(edge0, alertPatch3);
        alertPatches[9] = graph.getAlertPatches(edge0);

        assertEquals(0, alertPatches[0].length);
        assertFalse(contains (alertPatches[0], alertPatch0));
        assertFalse(contains (alertPatches[0], alertPatch1));
        assertFalse(contains (alertPatches[0], alertPatch2));
        assertFalse(contains (alertPatches[0], alertPatch3));
        assertEquals(0, alertPatches[1].length);
        assertFalse(contains (alertPatches[1], alertPatch0));
        assertFalse(contains (alertPatches[1], alertPatch1));
        assertFalse(contains (alertPatches[1], alertPatch2));
        assertFalse(contains (alertPatches[1], alertPatch3));
        assertEquals(1, alertPatches[2].length);
        assertTrue(contains (alertPatches[2], alertPatch0));
        assertTrue(contains (alertPatches[2], alertPatch1));
        assertFalse(contains (alertPatches[2], alertPatch2));
        assertFalse(contains (alertPatches[2], alertPatch3));
        assertEquals(0, alertPatches[3].length);
        assertFalse(contains (alertPatches[3], alertPatch0));
        assertFalse(contains (alertPatches[3], alertPatch1));
        assertFalse(contains (alertPatches[3], alertPatch2));
        assertFalse(contains (alertPatches[3], alertPatch3));
        assertEquals(1, alertPatches[4].length);
        assertTrue(contains (alertPatches[4], alertPatch0));
        assertTrue(contains (alertPatches[4], alertPatch1));
        assertFalse(contains (alertPatches[4], alertPatch2));
        assertFalse(contains (alertPatches[4], alertPatch3));
        assertEquals(0, alertPatches[5].length);
        assertFalse(contains (alertPatches[5], alertPatch0));
        assertFalse(contains (alertPatches[5], alertPatch1));
        assertFalse(contains (alertPatches[5], alertPatch2));
        assertFalse(contains (alertPatches[5], alertPatch3));
        assertEquals(2, alertPatches[6].length);
        assertFalse(contains (alertPatches[6], alertPatch0));
        assertFalse(contains (alertPatches[6], alertPatch1));
        assertTrue(contains (alertPatches[6], alertPatch2));
        assertTrue(contains (alertPatches[6], alertPatch3));
        assertEquals(1, alertPatches[7].length);
        assertFalse(contains (alertPatches[7], alertPatch0));
        assertFalse(contains (alertPatches[7], alertPatch1));
        assertFalse(contains (alertPatches[7], alertPatch2));
        assertTrue(contains (alertPatches[7], alertPatch3));
        assertEquals(3, alertPatches[8].length);
        assertTrue(contains (alertPatches[8], alertPatch0));
        assertTrue(contains (alertPatches[8], alertPatch1));
        assertTrue(contains (alertPatches[8], alertPatch2));
        assertTrue(contains (alertPatches[8], alertPatch3));
        assertEquals(0, alertPatches[9].length);
        assertFalse(contains (alertPatches[9], alertPatch0));
        assertFalse(contains (alertPatches[9], alertPatch1));
        assertFalse(contains (alertPatches[9], alertPatch2));
        assertFalse(contains (alertPatches[9], alertPatch3));
    }
}
