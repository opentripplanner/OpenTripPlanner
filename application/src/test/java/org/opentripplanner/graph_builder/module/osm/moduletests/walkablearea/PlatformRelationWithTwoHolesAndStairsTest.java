package org.opentripplanner.graph_builder.module.osm.moduletests.walkablearea;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.RelationBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;

/**
 * Checks that a platform with two inner holes (each connected to a stairway) produces a
 * fully-connected graph.
 *
 * <p>The platform is a ~200 m square "double-donut": one outer ring with two square holes cut out
 * side by side (west and east). Each hole's south boundary has a midpoint node (inner1S / inner2S)
 * where a stairway from outside the platform terminates.
 *
 * <p>A pedestrian footway from the north terminates at {@code ped} on the outer ring's north side.
 * This makes ped a startingNode, giving it visibility edges into both inner holes. The SPT from
 * {ped, inner1S, inner2S} keeps the edges that connect all three across the donut.
 *
 * <p>The two test cases share the same OSM geometry (built by {@link #buildSummarizer}) but use a
 * different {@code maxAreaNodes} budget. {@link #platformRelationConnectedToTwoStairways} uses a
 * generous budget (50) and gets the full, deterministic set of visibility edges.
 * {@link #lowMaxAreaNodes} uses a budget (5) smaller than the number of visibility-vertex
 * candidates, forcing the builder to sample the candidate set down. The mandatory ring, footway and
 * stair edges are unaffected by the budget; the surviving visibility cross-edges depend on the
 * sampling order and are not asserted (see that test for details).
 */
class PlatformRelationWithTwoHolesAndStairsTest {

  @Test
  void platformRelationConnectedToTwoStairways() {
    var summarizer = buildSummarizer(50);

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        // outer ring (5 segments × 2 directions) — ped splits the north side into two segments
        "(0,0) → (0.002,0) PEDESTRIAN ♿✅",
        "(0.002,0) → (0,0) PEDESTRIAN ♿✅",
        "(0.002,0) → (0.002,0.001) PEDESTRIAN ♿✅",
        "(0.002,0.001) → (0.002,0) PEDESTRIAN ♿✅",
        "(0.002,0.001) → (0.002,0.002) PEDESTRIAN ♿✅",
        "(0.002,0.002) → (0.002,0.001) PEDESTRIAN ♿✅",
        "(0.002,0.002) → (0,0.002) PEDESTRIAN ♿✅",
        "(0,0.002) → (0.002,0.002) PEDESTRIAN ♿✅",
        "(0,0.002) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,0.002) PEDESTRIAN ♿✅",
        // pedestrian footway from north
        "(0.002,0.001) → (0.0024,0.001) PEDESTRIAN ♿✅",
        "(0.0024,0.001) → (0.002,0.001) PEDESTRIAN ♿✅",
        // visibility edges: ped sees the innermost corner of each hole (the corner facing the
        // platform centre, not the outer wall), since those are the only corners reachable
        // without the line passing through the hole itself.
        "(0.002,0.001) → (0.0007,0.0008) PEDESTRIAN ♿✅",
        "(0.0007,0.0008) → (0.002,0.001) PEDESTRIAN ♿✅",
        "(0.002,0.001) → (0.0007,0.0012) PEDESTRIAN ♿✅",
        "(0.0007,0.0012) → (0.002,0.001) PEDESTRIAN ♿✅",
        // visibility edge: inner1S ↔ inner2S directly across the gap between the two holes
        "(0.0007,0.0006) → (0.0007,0.0014) PEDESTRIAN ♿✅",
        "(0.0007,0.0014) → (0.0007,0.0006) PEDESTRIAN ♿✅",
        // inner hole 1 (5 segments × 2 directions)
        "(0.0007,0.0004) → (0.0013,0.0004) PEDESTRIAN ♿✅",
        "(0.0013,0.0004) → (0.0007,0.0004) PEDESTRIAN ♿✅",
        "(0.0013,0.0004) → (0.0013,0.0008) PEDESTRIAN ♿✅",
        "(0.0013,0.0008) → (0.0013,0.0004) PEDESTRIAN ♿✅",
        "(0.0013,0.0008) → (0.0007,0.0008) PEDESTRIAN ♿✅",
        "(0.0007,0.0008) → (0.0013,0.0008) PEDESTRIAN ♿✅",
        "(0.0007,0.0008) → (0.0007,0.0006) PEDESTRIAN ♿✅",
        "(0.0007,0.0006) → (0.0007,0.0008) PEDESTRIAN ♿✅",
        "(0.0007,0.0006) → (0.0007,0.0004) PEDESTRIAN ♿✅",
        "(0.0007,0.0004) → (0.0007,0.0006) PEDESTRIAN ♿✅",
        // stair 1 — wheelchair-inaccessible steps
        "(0.0007,0.0006) → (-0.0003,0.0006) PEDESTRIAN ♿❌",
        "(-0.0003,0.0006) → (0.0007,0.0006) PEDESTRIAN ♿❌",
        // inner hole 2 (5 segments × 2 directions)
        "(0.0007,0.0012) → (0.0013,0.0012) PEDESTRIAN ♿✅",
        "(0.0013,0.0012) → (0.0007,0.0012) PEDESTRIAN ♿✅",
        "(0.0013,0.0012) → (0.0013,0.0016) PEDESTRIAN ♿✅",
        "(0.0013,0.0016) → (0.0013,0.0012) PEDESTRIAN ♿✅",
        "(0.0013,0.0016) → (0.0007,0.0016) PEDESTRIAN ♿✅",
        "(0.0007,0.0016) → (0.0013,0.0016) PEDESTRIAN ♿✅",
        "(0.0007,0.0016) → (0.0007,0.0014) PEDESTRIAN ♿✅",
        "(0.0007,0.0014) → (0.0007,0.0016) PEDESTRIAN ♿✅",
        "(0.0007,0.0014) → (0.0007,0.0012) PEDESTRIAN ♿✅",
        "(0.0007,0.0012) → (0.0007,0.0014) PEDESTRIAN ♿✅",
        // stair 2 — wheelchair-inaccessible steps
        "(0.0007,0.0014) → (-0.0003,0.0014) PEDESTRIAN ♿❌",
        "(-0.0003,0.0014) → (0.0007,0.0014) PEDESTRIAN ♿❌"
      );
  }

  @Test
  void lowMaxAreaNodes() {
    var summarizer = buildSummarizer(5);
    // The ring, footway and stair edges are created unconditionally, so they are always present and
    // identical to the maxAreaNodes=50 case. The visibility (cross) edges, however, depend on which
    // vertices the node budget samples, and that sampling follows the (identity-based) iteration
    // order of the candidate set, which is not stable across JVM runs. We therefore only assert the
    // deterministic mandatory edges here; the point of this case is that a budget smaller than the
    // candidate count does not drop any of them.
    assertWithMessage("Missing mandatory edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsAtLeast(
        // outer ring (5 segments × 2 directions) — ped splits the north side into two segments
        "(0,0) → (0.002,0) PEDESTRIAN ♿✅",
        "(0.002,0) → (0,0) PEDESTRIAN ♿✅",
        "(0.002,0) → (0.002,0.001) PEDESTRIAN ♿✅",
        "(0.002,0.001) → (0.002,0) PEDESTRIAN ♿✅",
        "(0.002,0.001) → (0.002,0.002) PEDESTRIAN ♿✅",
        "(0.002,0.002) → (0.002,0.001) PEDESTRIAN ♿✅",
        "(0.002,0.002) → (0,0.002) PEDESTRIAN ♿✅",
        "(0,0.002) → (0.002,0.002) PEDESTRIAN ♿✅",
        "(0,0.002) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,0.002) PEDESTRIAN ♿✅",
        // pedestrian footway from north
        "(0.002,0.001) → (0.0024,0.001) PEDESTRIAN ♿✅",
        "(0.0024,0.001) → (0.002,0.001) PEDESTRIAN ♿✅",
        // inner hole 1 (5 segments × 2 directions)
        "(0.0007,0.0004) → (0.0013,0.0004) PEDESTRIAN ♿✅",
        "(0.0013,0.0004) → (0.0007,0.0004) PEDESTRIAN ♿✅",
        "(0.0013,0.0004) → (0.0013,0.0008) PEDESTRIAN ♿✅",
        "(0.0013,0.0008) → (0.0013,0.0004) PEDESTRIAN ♿✅",
        "(0.0013,0.0008) → (0.0007,0.0008) PEDESTRIAN ♿✅",
        "(0.0007,0.0008) → (0.0013,0.0008) PEDESTRIAN ♿✅",
        "(0.0007,0.0008) → (0.0007,0.0006) PEDESTRIAN ♿✅",
        "(0.0007,0.0006) → (0.0007,0.0008) PEDESTRIAN ♿✅",
        "(0.0007,0.0006) → (0.0007,0.0004) PEDESTRIAN ♿✅",
        "(0.0007,0.0004) → (0.0007,0.0006) PEDESTRIAN ♿✅",
        // inner hole 2 (5 segments × 2 directions)
        "(0.0007,0.0012) → (0.0013,0.0012) PEDESTRIAN ♿✅",
        "(0.0013,0.0012) → (0.0007,0.0012) PEDESTRIAN ♿✅",
        "(0.0013,0.0012) → (0.0013,0.0016) PEDESTRIAN ♿✅",
        "(0.0013,0.0016) → (0.0013,0.0012) PEDESTRIAN ♿✅",
        "(0.0013,0.0016) → (0.0007,0.0016) PEDESTRIAN ♿✅",
        "(0.0007,0.0016) → (0.0013,0.0016) PEDESTRIAN ♿✅",
        "(0.0007,0.0016) → (0.0007,0.0014) PEDESTRIAN ♿✅",
        "(0.0007,0.0014) → (0.0007,0.0016) PEDESTRIAN ♿✅",
        "(0.0007,0.0014) → (0.0007,0.0012) PEDESTRIAN ♿✅",
        "(0.0007,0.0012) → (0.0007,0.0014) PEDESTRIAN ♿✅",
        // stairs — wheelchair-inaccessible steps, one per hole
        "(0.0007,0.0006) → (-0.0003,0.0006) PEDESTRIAN ♿❌",
        "(-0.0003,0.0006) → (0.0007,0.0006) PEDESTRIAN ♿❌",
        "(0.0007,0.0014) → (-0.0003,0.0014) PEDESTRIAN ♿❌",
        "(-0.0003,0.0014) → (0.0007,0.0014) PEDESTRIAN ♿❌"
      );
  }

  private static @NonNull GraphSummarizer buildSummarizer(int maxAreaNodes) {
    // Outer ring: ~200 m square
    var outerBL = node(0, new WgsCoordinate(0, 0));
    var outerTL = node(1, new WgsCoordinate(0.002, 0));
    var outerTR = node(2, new WgsCoordinate(0.002, 0.002));
    var outerBR = node(3, new WgsCoordinate(0, 0.002));
    var ped = node(4, new WgsCoordinate(0.002, 0.001));
    var outerRing = List.of(outerBL, outerTL, ped, outerTR, outerBR);

    var north = node(5, new WgsCoordinate(0.0024, 0.001));

    // Inner hole 1 (west): lat 0.0007–0.0013, lon 0.0004–0.0008
    var inner1BL = node(10, new WgsCoordinate(0.0007, 0.0004));
    var inner1TL = node(11, new WgsCoordinate(0.0013, 0.0004));
    var inner1TR = node(12, new WgsCoordinate(0.0013, 0.0008));
    var inner1BR = node(13, new WgsCoordinate(0.0007, 0.0008));
    var inner1S = node(14, new WgsCoordinate(0.0007, 0.0006));
    var innerHole1 = List.of(inner1BL, inner1TL, inner1TR, inner1BR, inner1S);

    // Inner hole 2 (east): lat 0.0007–0.0013, lon 0.0012–0.0016
    var inner2BL = node(20, new WgsCoordinate(0.0007, 0.0012));
    var inner2TL = node(21, new WgsCoordinate(0.0013, 0.0012));
    var inner2TR = node(22, new WgsCoordinate(0.0013, 0.0016));
    var inner2BR = node(23, new WgsCoordinate(0.0007, 0.0016));
    var inner2S = node(24, new WgsCoordinate(0.0007, 0.0014));
    var innerHole2 = List.of(inner2BL, inner2TL, inner2TR, inner2BR, inner2S);

    // Stairs from south, one per hole
    var stair1Bottom = node(30, new WgsCoordinate(-0.0003, 0.0006));
    var stair2Bottom = node(31, new WgsCoordinate(-0.0003, 0.0014));

    long outerRingId = 1000;
    long innerHole1Id = 1001;
    long innerHole2Id = 1002;

    var relation = RelationBuilder.ofMultiPolygon()
      .withTag("public_transport", "platform")
      .withOuterWay(outerRingId)
      .withInnerWay(innerHole1Id)
      .withInnerWay(innerHole2Id)
      .build();

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(way -> way.withTag("public_transport", "platform"), outerRingId, outerRing)
      .addAreaFromNodes(innerHole1Id, innerHole1)
      .addAreaFromNodes(innerHole2Id, innerHole2)
      .addWayFromNodes(way -> way.withTag("highway", "steps"), stair1Bottom, inner1S)
      .addWayFromNodes(way -> way.withTag("highway", "steps"), stair2Bottom, inner2S)
      .addWayFromNodes(way -> way.withTag("highway", "footway"), north, ped)
      .addRelation(relation)
      .build();

    var graph = new Graph();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withAreaVisibility(true)
      .withMaxAreaNodes(maxAreaNodes)
      .build()
      .buildGraph();

    return new GraphSummarizer(graph);
  }
}
