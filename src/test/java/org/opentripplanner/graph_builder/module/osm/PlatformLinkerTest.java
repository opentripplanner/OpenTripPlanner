package org.opentripplanner.graph_builder.module.osm;


import org.junit.Test;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.openstreetmap.BinaryOpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import java.io.File;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


public class PlatformLinkerTest {

    /**
     * Test linking from stairs endpoint to nodes in the ring defining the platform area.
     * OSM test data is from Skøyen station, Norway
     */
    @Test
    public void testLinkEntriesToPlatforms() throws Exception {

        String stairsEndpointLabel = "osm:node:1028861028";

        List<String> platformRingVertexLabels = Arrays.asList("osm:node:304045332", "osm:node:3238357455", "osm:node:1475363433",
                "osm:node:3238357491", "osm:node:1475363427", "osm:node:304045336", "osm:node:304045337", "osm:node:1475363437",
                "osm:node:3238357483", "osm:node:1475363443", "osm:node:1028860941", "osm:node:304045341", "osm:node:304045332");


        Graph gg = new Graph();
        OpenStreetMapModule loader = new OpenStreetMapModule();
        loader.platformEntriesLinking = true;
        loader.skipVisibility = false;
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());

        File file = new File(
                URLDecoder.decode(FakeGraph.class.getResource("osm/skoyen.osm.pbf").getFile(),
                        "UTF-8"));

        BinaryOpenStreetMapProvider provider = new BinaryOpenStreetMapProvider(file, false);

        loader.setProvider(provider);
        loader.buildGraph(gg, new HashMap<>());

        Vertex stairsEndpoint = gg.getVertex(stairsEndpointLabel);

        // verify outgoing links
        List<String> linkedRingVertecies = stairsEndpoint.getOutgoing().stream().map(edge -> edge.getToVertex().getLabel()).collect(Collectors.toList());
        assertEquals(linkedRingVertecies.size() -2, platformRingVertexLabels.size());  // the endpoint has links to two nodes in OSM
        for(String label : platformRingVertexLabels){
            assert(linkedRingVertecies.contains(label));
        }

        // verify incoming links
        List<String> linkedRingVerteciesInn = stairsEndpoint.getIncoming().stream().map(edge -> edge.getFromVertex().getLabel()).collect(Collectors.toList());
        assertEquals(linkedRingVerteciesInn.size() -2, platformRingVertexLabels.size());  // the endpoint has links to two nodes in OSM
        for(String label : platformRingVertexLabels){
            assert(linkedRingVerteciesInn.contains(label));
        }

    }

}
