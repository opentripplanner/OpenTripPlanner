package org.opentripplanner.graph_builder.module.islandpruning;

import java.io.File;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.openstreetmap.OsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

/* Test data consists of one bigger graph and two small sub graphs. These are totally disconnected.
   One small graphs is only at 5 meter distance from the big graph and another one 30 m away.
   Adaptive pruning retains the distant island but removes the closer one which appears to be
   disconnected part of the main graph.
 */

public class AdaptivePruningTest {

  private static Graph graph;

  @BeforeAll
  static void setup() {
    graph = buildOsmGraph(ConstantsForTests.ADAPTIVE_PRUNE_OSM);
  }

  @Test
  public void distantIslandIsRetained() {
    Assertions.assertTrue(
      graph
        .getStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .contains("73386383")
    );
  }

  @Test
  public void nearIslandIsRemoved() {
    Assertions.assertFalse(
      graph
        .getStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .contains("37751757")
    );
  }

  @Test
  public void mainGraphIsNotRemoved() {
    Assertions.assertTrue(
      graph
        .getStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .contains("73347312")
    );
  }

  private static Graph buildOsmGraph(String osmPath) {
    try {
      var deduplicator = new Deduplicator();
      var graph = new Graph(deduplicator);
      var transitModel = new TransitModel(new StopModel(), deduplicator);
      // Add street data from OSM
      File osmFile = new File(osmPath);
      OsmProvider osmProvider = new OsmProvider(osmFile, true);
      OsmModule osmModule = OsmModule
        .of(osmProvider, graph)
        .withCustomNamer(new TestNamer())
        .build();

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
      // all 3 sub graphs are larger than 5 edges
      pruneIslands.setPruningThresholdIslandWithoutStops(5);

      //  up to 5*20 = 100 edge graphs get pruned if they are too close
      pruneIslands.setAdaptivePruningFactor(20);

      //  Distant island is 30 m away from main graph, let's keep it
      pruneIslands.setAdaptivePruningDistance(30);

      pruneIslands.buildGraph();

      return graph;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
