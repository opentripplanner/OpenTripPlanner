package org.opentripplanner.graph_builder.module.osm.moduletests.walkablearea;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;

/**
 * Tests that stair connections at two diagonally opposite ring corners produce a surviving
 * diagonal visibility edge via the SPT pruning step.
 *
 * <p>Both corners are {@code isStartingNode} (shared with a stair way), so they enter
 * {@code visibilityVertices} and {@code startingVertices}. The diagonal visibility edge between
 * them is shorter than the two-segment ring path, so it survives the SPT pruning that
 * retains only edges on shortest paths between starting vertices.
 */
class DiagonalStairsOnRingCornersTest {

  @Test
  void diagonalVisibilityEdgeSurvivesPruning() {
    var bl = node(0, new WgsCoordinate(0, 0));
    var tl = node(1, new WgsCoordinate(0.001, 0));
    var tr = node(2, new WgsCoordinate(0.001, 0.001));
    var br = node(3, new WgsCoordinate(0, 0.001));
    var platform = List.of(bl, tl, tr, br);

    // Stairs at diagonally opposite corners bl and tr.
    var stairBL = node(10, new WgsCoordinate(-0.001, 0));
    var stairTR = node(11, new WgsCoordinate(0.002, 0.001));

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(way -> way.withTag("public_transport", "platform"), platform)
      .addWayFromNodes(way -> way.withTag("highway", "steps"), stairBL, bl)
      .addWayFromNodes(way -> way.withTag("highway", "steps"), stairTR, tr)
      .build();

    var graph = new Graph();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withAreaVisibility(true)
      .withMaxAreaNodes(50)
      .build()
      .buildGraph();

    var summarizer = new GraphSummarizer(graph);

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        // stairs at bl and tr (wheelchair-inaccessible steps)
        "(0,0) → (-0.001,0) PEDESTRIAN ♿❌",
        "(-0.001,0) → (0,0) PEDESTRIAN ♿❌",
        "(0.001,0.001) → (0.002,0.001) PEDESTRIAN ♿❌",
        "(0.002,0.001) → (0.001,0.001) PEDESTRIAN ♿❌",
        // ring segments (4 sides × 2 directions)
        "(0,0) → (0.001,0) PEDESTRIAN ♿✅",
        "(0.001,0) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,0.001) PEDESTRIAN ♿✅",
        "(0,0.001) → (0,0) PEDESTRIAN ♿✅",
        "(0.001,0) → (0.001,0.001) PEDESTRIAN ♿✅",
        "(0.001,0.001) → (0.001,0) PEDESTRIAN ♿✅",
        "(0.001,0.001) → (0,0.001) PEDESTRIAN ♿✅",
        "(0,0.001) → (0.001,0.001) PEDESTRIAN ♿✅",
        // surviving diagonal visibility edge bl↔tr (shorter than the two-segment ring detour)
        "(0,0) → (0.001,0.001) PEDESTRIAN ♿✅",
        "(0.001,0.001) → (0,0) PEDESTRIAN ♿✅"
      );
  }
}
