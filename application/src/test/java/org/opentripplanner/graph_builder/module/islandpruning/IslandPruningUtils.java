package org.opentripplanner.graph_builder.module.islandpruning;

import java.io.File;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.framework.Deduplicator;
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
      var deduplicator = new Deduplicator();
      var graph = new Graph(deduplicator);
      var timetableRepository = new TimetableRepository(new SiteRepository(), deduplicator);
      // Add street data from OSM
      OsmProvider osmProvider = new OsmProvider(osmFile, true);
      OsmModule osmModule = OsmModule.of(osmProvider, graph).withEdgeNamer(new TestNamer()).build();

      osmModule.buildGraph();

      timetableRepository.index();
      graph.index(timetableRepository.getSiteRepository());

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
