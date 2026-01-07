package org.opentripplanner.graph_builder.module.osm;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.ResourceLoader;

public class ElevatorNodeInAreaTest {

  private static final ResourceLoader RESOURCE_LOADER = ResourceLoader.of(ElevatorNodeInAreaTest.class);

  @Test
  public void testGraphBuilder() {
    var graph = new Graph();

    File file = RESOURCE_LOADER.file("wartenau.osm.pbf");

    DefaultOsmProvider provider = new DefaultOsmProvider(file, true);

    OsmModule osmModule = OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withAreaVisibility(true)
      .build();

    osmModule.buildGraph();
  }

}
