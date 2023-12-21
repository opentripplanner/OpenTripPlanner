package org.opentripplanner.graph_builder.module.islandpruning;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.openstreetmap.OsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public class EscalatorPruningTest {

  private static Graph graph;

  @BeforeAll
  static void setup() {
    graph =
      buildOsmGraph(ResourceLoader.of(EscalatorPruningTest.class).file("matinkyla-escalator.pbf"));
  }

  @Test
  public void streetEdgesBetweenEscalatorEdgesRetained() {
    Assertions.assertTrue(
      graph
        .getStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .containsAll(Set.of("490072445"))
    );
  }

  private static Graph buildOsmGraph(File osmFile) {
    try {
      var deduplicator = new Deduplicator();
      var graph = new Graph(deduplicator);
      var transitModel = new TransitModel(new StopModel(), deduplicator);
      // Add street data from OSM
      OsmProvider osmProvider = new OsmProvider(osmFile, true);
      OsmModule osmModule = OsmModule.of(osmProvider, graph).withEdgeNamer(new TestNamer()).build();

      osmModule.buildGraph();

      transitModel.index();
      graph.index(transitModel.getStopModel());

      // Prune floating islands and set noThru where necessary
      PruneIslands pruneIslands = new PruneIslands(
        graph,
        transitModel,
        DataImportIssueStore.NOOP,
        null
      );
      pruneIslands.setPruningThresholdIslandWithoutStops(40);
      pruneIslands.setPruningThresholdIslandWithStops(5);
      pruneIslands.setAdaptivePruningFactor(1);
      pruneIslands.buildGraph();

      return graph;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
