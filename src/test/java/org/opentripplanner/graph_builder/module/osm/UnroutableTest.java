package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.transit.model.framework.Deduplicator;

/**
 * Verify that OSM ways that represent proposed or as yet unbuilt roads are not used for routing.
 * This tests functionality in or around the method isWayRoutable() in the OSM graph builder
 * module.
 *
 * @author abyrd
 */
public class UnroutableTest {

  private Graph graph;

  @BeforeEach
  public void setUp() throws Exception {
    var deduplicator = new Deduplicator();
    graph = new Graph(deduplicator);

    URL osmDataUrl = getClass().getResource("bridge_construction.osm.pbf");
    File osmDataFile = new File(URLDecoder.decode(osmDataUrl.getFile(), StandardCharsets.UTF_8));
    OpenStreetMapProvider provider = new OpenStreetMapProvider(osmDataFile, true);
    OpenStreetMapModule osmBuilder = new OpenStreetMapModule(
      List.of(provider),
      Set.of(),
      graph,
      DataImportIssueStore.NOOP,
      true
    );
    osmBuilder.buildGraph();
  }

  /**
   * Search for a path across the Willamette river. This OSM data includes a bridge that is not yet
   * built and is therefore tagged highway=construction.
   * TODO also test unbuilt, proposed, raceways etc.
   */
  @Test
  public void testOnBoardRouting() {
    RouteRequest options = new RouteRequest();
    options.journey().direct().setMode(StreetMode.BIKE);

    Vertex from = graph.getVertex("osm:node:2003617278");
    Vertex to = graph.getVertex("osm:node:40446276");
    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder
      .of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setStreetRequest(options.journey().direct())
      .setFrom(from)
      .setTo(to)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = spt.getPath(to);
    // At the time of writing this test, the router simply doesn't find a path at all when highway=construction
    // is filtered out, thus the null check.
    if (path != null) {
      for (Edge edge : path.edges) {
        assertNotEquals(
          "Path should not use the as-yet unbuilt Tilikum Crossing bridge.",
          "Tilikum Crossing",
          edge.getDefaultName()
        );
      }
    }
  }
}
