package org.opentripplanner.graph_builder.module.osm.moduletests.walkablearea;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.RelationBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;

/**
 * Checks that a platform defined as a multipolygon relation (with an inner hole) connects
 * correctly when entries share a node with the platform boundary.
 *
 * <p>The platform is a square "donut" roughly 100 m across (0.0009° ≈ 100 m at the equator): a
 * large outer ring with a smaller square hole cut out of the middle. The inner ring's south side is
 * split with an intermediate node (innerS) where a stairway terminates.
 *
 * <p>A stairway from the south terminates at innerS on the inner hole boundary. Because innerS
 * lies on the inner hole boundary the donut polygon's strict contains() check fails, so innerS is
 * NOT a platformLinkingPoint. However, innerS is shared with the stair way, so
 * {@code isStartingNode} returns true, adding it to visibilityVertices.
 *
 * <p>A pedestrian footway from the north terminates at {@code ped}, a node on the outer ring's
 * north side that is shared with the footway. Like innerS, ped becomes a startingNode, so it
 * enters visibilityVertices. The SPT from {ped, innerS} finds a path via ped↔innerTR visibility
 * edges, keeping them alive after pruning.
 */
class PlatformRelationWithHoleAndStairsTest {

  @Test
  void platformRelationConnectedToStairway() {
    // Pedestrian footway from north: north is the external entry, ped is a node on the outer
    // ring's north side. Being shared with the footway makes ped a startingNode, so it enters
    // visibilityVertices and gets connected to the inner ring via a visibility edge.
    var north = node(21, new WgsCoordinate(0.0011, 0.000451));
    var ped = node(22, new WgsCoordinate(0.0009, 0.0004511));

    // Outer ring: ~100 m square (0.0009° ≈ 100 m at the equator)
    var outerBL = node(0, new WgsCoordinate(0, 0));
    var outerTL = node(1, new WgsCoordinate(0.0009, 0));
    var outerTR = node(2, new WgsCoordinate(0.0009, 0.0009));
    var outerBR = node(3, new WgsCoordinate(0, 0.0009));
    var outerRing = List.of(outerBL, outerTL, ped, outerTR, outerBR);

    // Inner hole: ~33 m square centred inside the outer ring.
    // The south side (innerBR→innerBL) is split by an intermediate node innerS at the midpoint.
    var innerBL = node(10, new WgsCoordinate(0.0003, 0.0003));
    var innerTL = node(11, new WgsCoordinate(0.0006, 0.0003));
    var innerTR = node(12, new WgsCoordinate(0.0006, 0.0006));
    var innerBR = node(13, new WgsCoordinate(0.0003, 0.0006));
    var innerS = node(14, new WgsCoordinate(0.0003, 0.000451));
    var innerHole = List.of(innerBL, innerTL, innerTR, innerBR, innerS);

    // Stair from south: terminates at innerS on the inner hole boundary.
    // innerS fails the donut contains() check so it is NOT a platformLinkingPoint,
    // but isStartingNode returns true (shared with stair way) → innerS enters visibilityVertices.
    var stairBottom = node(20, new WgsCoordinate(-0.0001, 0.000451));

    long outerRingId = 1000;
    long innerHoleId = 1001;

    var relation = RelationBuilder.ofMultiPolygon()
      .withTag("public_transport", "platform")
      .withOuterWay(outerRingId)
      .withInnerWay(innerHoleId)
      .build();

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(way -> way.withTag("public_transport", "platform"), outerRingId, outerRing)
      .addAreaFromNodes(innerHoleId, innerHole)
      .addWayFromNodes(way -> way.withTag("highway", "steps"), stairBottom, innerS)
      .addWayFromNodes(way -> way.withTag("highway", "footway"), north, ped)
      .addRelation(relation)
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
        // outer ring (5 segments × 2 directions) — ped splits the north side into two segments
        "(0,0) → (0.0009,0) PEDESTRIAN ♿✅",
        "(0.0009,0) → (0,0) PEDESTRIAN ♿✅",
        "(0.0009,0) → (0.0009,0.000451) PEDESTRIAN ♿✅",
        "(0.0009,0.000451) → (0.0009,0) PEDESTRIAN ♿✅",
        "(0.0009,0.000451) → (0.0009,0.0009) PEDESTRIAN ♿✅",
        "(0.0009,0.0009) → (0.0009,0.000451) PEDESTRIAN ♿✅",
        "(0.0009,0.0009) → (0,0.0009) PEDESTRIAN ♿✅",
        "(0,0.0009) → (0.0009,0.0009) PEDESTRIAN ♿✅",
        "(0,0.0009) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,0.0009) PEDESTRIAN ♿✅",
        // inner hole (5 segments × 2 directions)
        "(0.0003,0.0003) → (0.0006,0.0003) PEDESTRIAN ♿✅",
        "(0.0006,0.0003) → (0.0003,0.0003) PEDESTRIAN ♿✅",
        "(0.0006,0.0003) → (0.0006,0.0006) PEDESTRIAN ♿✅",
        "(0.0006,0.0006) → (0.0006,0.0003) PEDESTRIAN ♿✅",
        "(0.0006,0.0006) → (0.0003,0.0006) PEDESTRIAN ♿✅",
        "(0.0003,0.0006) → (0.0006,0.0006) PEDESTRIAN ♿✅",
        "(0.0003,0.0006) → (0.0003,0.000451) PEDESTRIAN ♿✅",
        "(0.0003,0.000451) → (0.0003,0.0006) PEDESTRIAN ♿✅",
        "(0.0003,0.000451) → (0.0003,0.0003) PEDESTRIAN ♿✅",
        "(0.0003,0.0003) → (0.0003,0.000451) PEDESTRIAN ♿✅",
        // visibility edges: ped ↔ innerTR (NE corner of inner hole), both directions survive.
        // The lon offset of 0.000451 breaks the path-length tie that existed at 0.00045,
        // so the SPT finds a unique shortest path in both directions through innerTR.
        "(0.0009,0.000451) → (0.0006,0.0006) PEDESTRIAN ♿✅",
        "(0.0006,0.0006) → (0.0009,0.000451) PEDESTRIAN ♿✅",
        // stair — wheelchair-inaccessible steps
        "(0.0003,0.000451) → (-0.0001,0.000451) PEDESTRIAN ♿❌",
        "(-0.0001,0.000451) → (0.0003,0.000451) PEDESTRIAN ♿❌",
        // pedestrian footway from north
        "(0.0011,0.000451) → (0.0009,0.000451) PEDESTRIAN ♿✅",
        "(0.0009,0.000451) → (0.0011,0.000451) PEDESTRIAN ♿✅"
      );
  }
}
