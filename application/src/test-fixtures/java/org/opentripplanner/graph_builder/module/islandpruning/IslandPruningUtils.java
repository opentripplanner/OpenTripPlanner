package org.opentripplanner.graph_builder.module.islandpruning;

import java.io.File;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;

class IslandPruningUtils {

  static Graph buildOsmGraph(File osmFile, IslandPruningParameters parameters) {
    try {
      var graph = new Graph();
      var osmProvider = new DefaultOsmProvider(osmFile, true);

      var osmModule = OsmModuleTestFactory.of(osmProvider)
        .withGraph(graph)
        .builder()
        .withEdgeNamer(new TestNamer())
        .build();

      osmModule.buildGraph();

      prune(graph, parameters);

      return graph;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Runs {@link IslandPruningModule} against an already-built graph.
   */
  static void prune(Graph graph, IslandPruningParameters parameters) {
    var transitRepository = new TransitRepository(new SiteRepository());
    transitRepository.index();
    graph.index();

    IslandPruningModule pruneIslands = new IslandPruningModule(
      graph,
      transitRepository,
      DataImportIssueStore.NOOP,
      null,
      parameters
    );
    pruneIslands.buildGraph();
  }
}
