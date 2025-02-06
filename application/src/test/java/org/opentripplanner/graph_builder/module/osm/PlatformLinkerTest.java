package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class PlatformLinkerTest {

  /**
   * Test linking from stairs endpoint to nodes in the ring defining the platform area. OSM test
   * data is from Skøyen station, Norway
   */
  @Test
  public void testLinkEntriesToPlatforms() {
    var stairsEndpointLabel = VertexLabel.osm(1028861028);

    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);

    File file = ResourceLoader.of(this).file("skoyen.osm.pbf");

    DefaultOsmProvider provider = new DefaultOsmProvider(file, false);

    OsmModule osmModule = OsmModule
      .of(
        provider,
        graph,
        new DefaultOsmInfoGraphBuildRepository(),
        new DefaultVehicleParkingRepository()
      )
      .withPlatformEntriesLinking(true)
      .build();

    osmModule.buildGraph();

    Vertex stairsEndpoint = graph.getVertex(stairsEndpointLabel);

    // verify outgoing links
    assertTrue(stairsEndpoint.getOutgoing().stream().anyMatch(AreaEdge.class::isInstance));

    // verify incoming links
    assertTrue(stairsEndpoint.getIncoming().stream().anyMatch(AreaEdge.class::isInstance));
  }
}
