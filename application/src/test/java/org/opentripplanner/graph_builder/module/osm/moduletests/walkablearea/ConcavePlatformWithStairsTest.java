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
 * Tests that a re-entrant (concave) corner of an L-shaped platform enters the visibility vertex
 * set via {@code isNodeConvex}, enabling the shortest path between two stair entries.
 *
 * <p>The platform is an L-shape made of two rectangles:
 * <pre>
 *   A --- B
 *   |     C --- D
 *   E ---------- F
 * </pre>
 * Corner C is the re-entrant corner (interior angle &gt; 180° in the polygon sense).
 * {@code Ring.isNodeConvex(C)} returns {@code true} for a CW ring because the cross-product at
 * a re-entrant corner is positive. This adds C to {@code visibilityVertices}.
 *
 * <p>Stairs connect from outside to the top-left (A) and the bottom-right (F). The direct
 * line-of-sight from A to F grazes the re-entrant corner C exactly (the segment A–F passes
 * through C's coordinate), so it stays inside the L-shape and is a valid visibility edge. Being
 * shorter than the ring detour A→E→F, this direct diagonal survives the SPT pruning between the
 * two stair entries; no separate A–C or C–F visibility edges are needed.
 */
class ConcavePlatformWithStairsTest {

  @Test
  void reEntrantCornerEnablesShortestPath() {
    // L-shape: left column (lat 0–0.004, lon 0–0.002) + bottom row (lat 0–0.002, lon 0–0.004)
    var a = node(0, new WgsCoordinate(0.004, 0));
    var b = node(1, new WgsCoordinate(0.004, 0.002));
    var c = node(2, new WgsCoordinate(0.002, 0.002));
    var d = node(3, new WgsCoordinate(0.002, 0.004));
    var f = node(4, new WgsCoordinate(0, 0.004));
    var e = node(5, new WgsCoordinate(0, 0));
    var lShape = List.of(a, b, c, d, f, e);

    // Stairs: one entering from above-left (reaches A), one from below-right (reaches F).
    // The direct A–F line passes through the cut-out quadrant, so C is the connecting relay.
    var stairA = node(10, new WgsCoordinate(0.005, 0));
    var stairF = node(11, new WgsCoordinate(0, 0.005));

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(way -> way.withTag("public_transport", "platform"), lShape)
      .addWayFromNodes(way -> way.withTag("highway", "steps"), stairA, a)
      .addWayFromNodes(way -> way.withTag("highway", "steps"), stairF, f)
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
        // stairs into A (wheelchair-inaccessible steps)
        "(0.004,0) → (0.005,0) PEDESTRIAN ♿❌",
        "(0.005,0) → (0.004,0) PEDESTRIAN ♿❌",
        // stairs into F
        "(0,0.004) → (0,0.005) PEDESTRIAN ♿❌",
        "(0,0.005) → (0,0.004) PEDESTRIAN ♿❌",
        // ring segments (6 sides × 2 directions)
        "(0.004,0) → (0.004,0.002) PEDESTRIAN ♿✅",
        "(0.004,0.002) → (0.004,0) PEDESTRIAN ♿✅",
        "(0.004,0.002) → (0.002,0.002) PEDESTRIAN ♿✅",
        "(0.002,0.002) → (0.004,0.002) PEDESTRIAN ♿✅",
        "(0.002,0.002) → (0.002,0.004) PEDESTRIAN ♿✅",
        "(0.002,0.004) → (0.002,0.002) PEDESTRIAN ♿✅",
        "(0.002,0.004) → (0,0.004) PEDESTRIAN ♿✅",
        "(0,0.004) → (0.002,0.004) PEDESTRIAN ♿✅",
        "(0,0.004) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,0.004) PEDESTRIAN ♿✅",
        "(0.004,0) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0.004,0) PEDESTRIAN ♿✅",
        // visibility diagonal A↔F grazing the re-entrant corner C — the surviving shortest path
        "(0.004,0) → (0,0.004) PEDESTRIAN ♿✅",
        "(0,0.004) → (0.004,0) PEDESTRIAN ♿✅"
      );
  }
}
