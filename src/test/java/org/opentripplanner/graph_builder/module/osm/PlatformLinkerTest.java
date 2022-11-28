package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class PlatformLinkerTest {

  /**
   * Test linking from stairs endpoint to nodes in the ring defining the platform area. OSM test
   * data is from Skøyen station, Norway
   */
  @Test
  public void testLinkEntriesToPlatforms() {
    String stairsEndpointLabel = "osm:node:1028861028";

    var deduplicator = new Deduplicator();
    var gg = new Graph(deduplicator);

    File file = new File(
      URLDecoder.decode(
        FakeGraph.class.getResource("osm/skoyen.osm.pbf").getFile(),
        StandardCharsets.UTF_8
      )
    );

    OpenStreetMapProvider provider = new OpenStreetMapProvider(file, false);

    OpenStreetMapModule osmModule = new OpenStreetMapModule(
      List.of(provider),
      Set.of(),
      gg,
      DataImportIssueStore.NOOP,
      true
    );
    osmModule.platformEntriesLinking = true;

    osmModule.buildGraph();

    Vertex stairsEndpoint = gg.getVertex(stairsEndpointLabel);

    // verify outgoing links
    assertTrue(stairsEndpoint.getOutgoing().stream().anyMatch(AreaEdge.class::isInstance));

    // verify incoming links
    assertTrue(stairsEndpoint.getIncoming().stream().anyMatch(AreaEdge.class::isInstance));
  }
}
