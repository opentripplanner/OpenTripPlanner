package org.opentripplanner.graph_builder.module.islandpruning;

import java.io.File;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

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
      var timetableRepository = new TimetableRepository(new SiteRepository());
      // Add street data from OSM
      var osmProvider = new DefaultOsmProvider(osmFile, true);

      var osmModule = OsmModuleTestFactory.of(osmProvider)
        .withGraph(graph)
        .builder()
        .withEdgeNamer(new TestNamer())
        .build();

      osmModule.buildGraph();

      timetableRepository.index();
      graph.index();

      // Prune floating islands and set noThru where necessary
      PruneIslands pruneIslands = new PruneIslands(
        graph,
        timetableRepository,
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
