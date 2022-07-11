package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public class PlatformLinkerTest {

  /**
   * Test linking from stairs endpoint to nodes in the ring defining the platform area. OSM test
   * data is from Skøyen station, Norway
   */
  @Test
  public void testLinkEntriesToPlatforms() {
    String stairsEndpointLabel = "osm:node:1028861028";

    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    var gg = new Graph(stopModel, deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);

    File file = new File(
      URLDecoder.decode(
        FakeGraph.class.getResource("osm/skoyen.osm.pbf").getFile(),
        StandardCharsets.UTF_8
      )
    );

    OpenStreetMapProvider provider = new OpenStreetMapProvider(file, false);

    OpenStreetMapModule osmModule = new OpenStreetMapModule(provider);
    osmModule.platformEntriesLinking = true;
    osmModule.skipVisibility = false;
    osmModule.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());

    osmModule.buildGraph(gg, transitModel, new HashMap<>());

    Vertex stairsEndpoint = gg.getVertex(stairsEndpointLabel);

    // verify outgoing links
    assertTrue(stairsEndpoint.getOutgoing().stream().anyMatch(AreaEdge.class::isInstance));

    // verify incoming links
    assertTrue(stairsEndpoint.getIncoming().stream().anyMatch(AreaEdge.class::isInstance));
  }
}
