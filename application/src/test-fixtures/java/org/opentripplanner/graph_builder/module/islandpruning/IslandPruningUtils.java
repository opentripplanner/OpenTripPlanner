package org.opentripplanner.graph_builder.module.islandpruning;

import java.io.File;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;

class IslandPruningUtils {

  static GraphSummarizer buildOsmGraph(File osmFile, IslandPruningParameters parameters) {
    try {
      var graph = buildStreetGraph(osmFile);
      prune(graph, parameters);
      return new GraphSummarizer(graph);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Builds a street graph from a single OSM file, without running island pruning. Split out of
   * {@link #buildOsmGraph} so callers (eg. a standalone benchmark) can time OSM import and island
   * pruning separately.
   */
  static Graph buildStreetGraph(File osmFile) {
    var graph = new Graph();
    var osmProvider = new DefaultOsmProvider(osmFile, true);

    var osmModule = OsmModuleTestFactory.of(osmProvider)
      .withGraph(graph)
      .builder()
      .withEdgeNamer(new TestNamer())
      .build();

    osmModule.buildGraph();
    return graph;
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
