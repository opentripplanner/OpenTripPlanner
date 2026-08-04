package org.opentripplanner.graph_builder.module.islandpruning;

import java.io.File;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;

class IslandPruningUtils {

  static Graph buildOsmGraph(
    File osmFile,
    int thresholdIslandWithoutStops,
    int thresholdIslandWithStops,
    double adaptivePruningFactor,
    int adaptivePruningDistance
  ) {
    try {
      var graph = new Graph();
      var transitRepository = new TransitRepository(new SiteRepository());
      // Add street data from OSM
      var osmProvider = new DefaultOsmProvider(osmFile, true);

      var osmModule = OsmModuleTestFactory.of(osmProvider)
        .withGraph(graph)
        .builder()
        .withEdgeNamer(new TestNamer())
        .build();

      osmModule.buildGraph();

      transitRepository.index();
      graph.index();

      // Prune floating islands and set noThru where necessary
      PruneIslands pruneIslands = new PruneIslands(
        graph,
        transitRepository,
        DataImportIssueStore.NOOP,
        null
      );
      pruneIslands.setPruningThresholdIslandWithoutStops(thresholdIslandWithoutStops);
      pruneIslands.setPruningThresholdIslandWithStops(thresholdIslandWithStops);
      pruneIslands.setAdaptivePruningFactor(adaptivePruningFactor);
      pruneIslands.setAdaptivePruningDistance(adaptivePruningDistance);
      pruneIslands.buildGraph();

      return graph;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
