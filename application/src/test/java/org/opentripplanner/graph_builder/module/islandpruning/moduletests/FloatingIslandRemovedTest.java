package org.opentripplanner.graph_builder.module.islandpruning.moduletests;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.buildStreetGraph;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.prune;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.summary.GraphSummarizer;

/**
 * A small street network that has no connection at all to the main street network is entirely
 * removed from the graph, since it is too small to be a plausible independent street network.
 */
class FloatingIslandRemovedTest {

  @Test
  void floatingIslandIsRemoved() {
    // Main street network: a small square of four intersections, large enough to never be
    // considered for pruning.
    var a = node(0, new WgsCoordinate(0, 0));
    var b = node(1, new WgsCoordinate(0, 1));
    var c = node(2, new WgsCoordinate(1, 1));
    var d = node(3, new WgsCoordinate(1, 0));

    // Floating island: two nodes, completely disconnected from the main street network.
    var i0 = node(4, new WgsCoordinate(10, 10));
    var i1 = node(5, new WgsCoordinate(10, 11));

    var provider = TestOsmProvider.of()
      .addWayFromNodes(a, b)
      .addWayFromNodes(b, c)
      .addWayFromNodes(c, d)
      .addWayFromNodes(d, a)
      .addWayFromNodes(i0, i1)
      .build();

    var graph = buildStreetGraph(provider);
    // Islands without stops smaller than 3 street vertices are pruned.
    prune(graph, 3, 3, 1, 250);

    var summarizer = new GraphSummarizer(graph);

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        "(0,0) → (0,1) PEDESTRIAN ♿✅",
        "(0,1) → (0,0) PEDESTRIAN ♿✅",
        "(0,1) → (1,1) PEDESTRIAN ♿✅",
        "(1,1) → (0,1) PEDESTRIAN ♿✅",
        "(1,1) → (1,0) PEDESTRIAN ♿✅",
        "(1,0) → (1,1) PEDESTRIAN ♿✅",
        "(1,0) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (1,0) PEDESTRIAN ♿✅"
      );
  }
}
