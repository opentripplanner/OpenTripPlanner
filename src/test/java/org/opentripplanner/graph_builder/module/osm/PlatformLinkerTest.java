package org.opentripplanner.graph_builder.module.osm;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.junit.Test;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

public class PlatformLinkerTest {

  /**
   * Test linking from stairs endpoint to nodes in the ring defining the platform area. OSM test
   * data is from Skøyen station, Norway
   */
  @Test
  public void testLinkEntriesToPlatforms() throws Exception {
    String stairsEndpointLabel = "osm:node:1028861028";

    Graph gg = new Graph();
    OpenStreetMapModule loader = new OpenStreetMapModule();
    loader.platformEntriesLinking = true;
    loader.skipVisibility = false;
    loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());

    File file = new File(
      URLDecoder.decode(
        FakeGraph.class.getResource("osm/skoyen.osm.pbf").getFile(),
        StandardCharsets.UTF_8
      )
    );

    BinaryOpenStreetMapProvider provider = new BinaryOpenStreetMapProvider(file, false);

    loader.setProvider(provider);
    loader.buildGraph(gg, new HashMap<>());

    Vertex stairsEndpoint = gg.getVertex(stairsEndpointLabel);

    // verify outgoing links
    assertTrue(stairsEndpoint.getOutgoing().stream().anyMatch(AreaEdge.class::isInstance));

    // verify incoming links
    assertTrue(stairsEndpoint.getIncoming().stream().anyMatch(AreaEdge.class::isInstance));
  }
}
