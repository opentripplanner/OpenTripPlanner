package org.opentripplanner.graph_builder.module.islandpruning;

import java.io.File;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.openstreetmap.OsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

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
