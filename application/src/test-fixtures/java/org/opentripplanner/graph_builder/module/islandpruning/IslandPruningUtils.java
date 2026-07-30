package org.opentripplanner.graph_builder.module.islandpruning;

import java.io.File;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public class IslandPruningUtils {

  public static Graph buildOsmGraph(File osmFile, IslandPruningParameters parameters) {
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
   * Builds the street graph from a synthetic {@link OsmProvider}, without running island
   * pruning. Callers that need to attach transit stops before pruning runs should call
   * {@link #prune} separately once the graph has been fully assembled.
   */
  public static Graph buildStreetGraph(OsmProvider provider) {
    var graph = new Graph();
    var osmModule = OsmModuleTestFactory.of(provider).withGraph(graph).builder().build();
    osmModule.buildGraph();
    return graph;
  }

  /**
   * Runs {@link IslandPruningModule} against an already-built graph.
   */
  public static void prune(Graph graph, IslandPruningParameters parameters) {
    var timetableRepository = new TimetableRepository(new SiteRepository());
    timetableRepository.index();
    graph.index();

    IslandPruningModule pruneIslands = new IslandPruningModule(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      null,
      parameters
    );
    pruneIslands.buildGraph();
  }
}
