package org.opentripplanner.routing.graph;

import org.junit.Test;
import org.opentripplanner.routing.alertpatch.AlertPatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.util.ArrayUtils.contains;

public class GraphAlertPatchTest {
    @Test
    public final void testAlertPatch() {
        AlertPatch[] alertPatches;
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


        alertPatches = graph.getAlertPatches(null);
        assertEquals(0, alertPatches.length);

        alertPatches = graph.getAlertPatches(edge0);
        assertEquals(0, alertPatches.length);

        graph.addAlertPatch(edge0, alertPatch0);
        alertPatches = graph.getAlertPatches(edge0);

        assertEquals(1, alertPatches.length);
        assertTrue(contains (alertPatches, alertPatch0));
        assertTrue(contains (alertPatches, alertPatch1));
        assertFalse(contains (alertPatches, alertPatch2));


        graph.addAlertPatch(edge0, alertPatch1);
        alertPatches = graph.getAlertPatches(edge1);
        assertEquals(0, alertPatches.length);

        graph.addAlertPatch(edge1, alertPatch3);
        alertPatches = graph.getAlertPatches(edge0);
        assertEquals(1, alertPatches.length);
        assertTrue(contains (alertPatches, alertPatch0));
        assertFalse(contains (alertPatches, alertPatch2));


        graph.removeAlertPatch(edge0, alertPatch1);
        alertPatches = graph.getAlertPatches(edge0);
        assertEquals(0, alertPatches.length);

        graph.addAlertPatch(edge1, alertPatch2);
        alertPatches = graph.getAlertPatches(edge1);
        assertEquals(2, alertPatches.length);
        assertFalse(contains (alertPatches, alertPatch0));
        assertTrue(contains (alertPatches, alertPatch2));
        assertTrue(contains (alertPatches, alertPatch3));

        graph.removeAlertPatch(edge1, alertPatch2);
        graph.removeAlertPatch(edge1, alertPatch2);
        alertPatches = graph.getAlertPatches(edge1);
        assertEquals(1, alertPatches.length);
        assertFalse(contains (alertPatches, alertPatch0));
        assertTrue(contains (alertPatches, alertPatch3));


        graph.addAlertPatch(edge0, alertPatch0);
        graph.addAlertPatch(edge0, alertPatch1);
        graph.addAlertPatch(edge0, alertPatch2);
        graph.addAlertPatch(edge0, alertPatch3);
        alertPatches = graph.getAlertPatches(edge0);
        assertEquals(3, alertPatches.length);
        assertTrue(contains (alertPatches, alertPatch0));
        assertTrue(contains (alertPatches, alertPatch1));
        assertTrue(contains (alertPatches, alertPatch2));
        assertTrue(contains (alertPatches, alertPatch3));
    }
}
