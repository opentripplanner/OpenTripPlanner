package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.google.common.collect.Maps;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.astar.AStarBuilder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * Verify that OSM ways that represent proposed or as yet unbuilt roads are not used for routing.
 * This tests functionality in or around the method isWayRoutable() in the OSM graph builder
 * module.
 *
 * @author abyrd
 */
public class TestUnroutable {

  private final Graph graph = new Graph();

  @BeforeEach
  public void setUp() throws Exception {
    OpenStreetMapModule osmBuilder = new OpenStreetMapModule();
    osmBuilder.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
    URL osmDataUrl = getClass().getResource("bridge_construction.osm.pbf");
    File osmDataFile = new File(URLDecoder.decode(osmDataUrl.getFile(), StandardCharsets.UTF_8));
    BinaryOSMProvider provider = new BinaryOSMProvider(osmDataFile, true);
    osmBuilder.setProvider(provider);
    HashMap<Class<?>, Object> extra = Maps.newHashMap();
    osmBuilder.buildGraph(graph, extra); // TODO get rid of this "extra" thing
  }

  /**
   * Search for a path across the Willamette river. This OSM data includes a bridge that is not yet
   * built and is therefore tagged highway=construction.
   * TODO also test unbuilt, proposed, raceways etc.
   */
  @Test
  public void testOnBoardRouting() {
    RoutingRequest options = new RoutingRequest();

    Vertex from = graph.getVertex("osm:node:2003617278");
    Vertex to = graph.getVertex("osm:node:40446276");
    options.setMode(TraverseMode.BICYCLE);
    ShortestPathTree spt = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, from, to))
      .getShortestPathTree();

    GraphPath path = spt.getPath(to);
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
