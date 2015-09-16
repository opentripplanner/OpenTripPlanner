package org.opentripplanner.streets;

import com.conveyal.osmlib.OSM;
import junit.framework.TestCase;
import org.junit.Test;

public class StreetLayerTest extends TestCase {

    /** Test that subgraphs are removed as expected */
    @Test
    public void testSubgraphRemoval () {
        OSM osm = new OSM(null);
        osm.intersectionDetection = true;
        osm.readFromUrl(StreetLayerTest.class.getResource("subgraph.vex").toString());

        StreetLayer sl = new StreetLayer();
        // load from OSM and don't remove floating subgraphs
        sl.loadFromOsm(osm, false, true);

        sl.buildEdgeLists();

        // find an edge that should be removed
        int v = sl.vertexIndexForOsmNode.get(961011556);
        assertEquals(3, sl.incomingEdges.get(v).size());
        assertEquals(3, sl.outgoingEdges.get(v).size());

        int e0 = sl.incomingEdges.get(v).get(0);
        int e1 = e0 % 2 == 0 ? e0 + 1 : e0 - 1;

        assertEquals(v, sl.edgeStore.getCursor(e0).getToVertex());
        assertEquals(v, sl.edgeStore.getCursor(e1).getFromVertex());

        sl.removeDisconnectedSubgraphs(40);

        // note: vertices of disconnected subgraphs are not removed
        assertEquals(0, sl.incomingEdges.get(v).size());
        assertEquals(0, sl.outgoingEdges.get(v).size());

        assertTrue(v != sl.edgeStore.getCursor(e0).getToVertex());
        assertTrue(v != sl.edgeStore.getCursor(e1).getFromVertex());
    }
}