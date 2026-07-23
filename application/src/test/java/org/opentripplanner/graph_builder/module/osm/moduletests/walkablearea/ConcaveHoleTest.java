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
 * Checks that concave areas of inner rings/holes in multipolygons are connected to the outer ring.
 * This is to avoid a situation where you cannot route out of the concave part (of the inner ring).
 * <p>
 * An example of such a geometry is https://www.openstreetmap.org/relation/8513460.
 * <p>
 * There we want to make sure that the node https://www.openstreetmap.org/node/6136980344
 * can be used as the start point of the search, and you can leave the area.
 * <p>
 * Further reading: https://github.com/opentripplanner/OpenTripPlanner/pull/6486
 */
class ConcaveHoleTest {

  @Test
  void visibilityNodes() {
    var inside0 = node(0, new WgsCoordinate(0, 0));
    var inside1 = node(2, new WgsCoordinate(5, 5));
    var outerRing = List.of(
      inside0,
      node(1, new WgsCoordinate(0, 5)),
      inside1,
      node(4, new WgsCoordinate(5, 0))
    );

    int visibilityNodeId = 107;
    var hole = List.of(
      node(100, new WgsCoordinate(1, 1)),
      node(101, new WgsCoordinate(1, 4)),
      node(102, new WgsCoordinate(4, 4)),
      node(103, new WgsCoordinate(4, 3)),
      node(104, new WgsCoordinate(3, 3)),
      node(105, new WgsCoordinate(3, 2)),
      node(106, new WgsCoordinate(4, 2)),
      // this is the node in the hole that will be connected to the outer ring
      node(visibilityNodeId, new WgsCoordinate(4, 1))
    );

    var outside0 = node(5, new WgsCoordinate(-1, 0));
    var outside1 = node(6, new WgsCoordinate(6, 5));

    var outerRingId = 1000;
    var holeId = 1001;

    var relation = RelationBuilder.ofMultiPolygon()
      .withTag("highway", "pedestrian")
      .withWayMember(outerRingId, "outer")
      .withWayMember(holeId, "inner")
      .build();

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(outerRingId, outerRing)
      .addAreaFromNodes(holeId, hole)
      .addWayFromNodes(outside0, inside0)
      .addWayFromNodes(outside1, inside1)
      .addRelation(relation)
      .build();

    var graph = new Graph();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withAreaVisibility(true)
      .withMaxAreaNodes(10)
      .build()
      .buildGraph();

    var summarizer = new GraphSummarizer(graph);

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        // connecting ways from outside into two outer-ring corners
        "(0,0) → (-1,0) PEDESTRIAN ♿✅",
        "(-1,0) → (0,0) PEDESTRIAN ♿✅",
        "(5,5) → (6,5) PEDESTRIAN ♿✅",
        "(6,5) → (5,5) PEDESTRIAN ♿✅",
        // outer ring (4 sides × 2 directions)
        "(0,0) → (5,0) PEDESTRIAN ♿✅",
        "(5,0) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,5) PEDESTRIAN ♿✅",
        "(0,5) → (0,0) PEDESTRIAN ♿✅",
        "(0,5) → (5,5) PEDESTRIAN ♿✅",
        "(5,5) → (0,5) PEDESTRIAN ♿✅",
        "(5,5) → (5,0) PEDESTRIAN ♿✅",
        "(5,0) → (5,5) PEDESTRIAN ♿✅",
        // concave (comb-shaped) hole ring (8 sides × 2 directions)
        "(1,1) → (4,1) PEDESTRIAN ♿✅",
        "(4,1) → (1,1) PEDESTRIAN ♿✅",
        "(1,1) → (1,4) PEDESTRIAN ♿✅",
        "(1,4) → (1,1) PEDESTRIAN ♿✅",
        "(1,4) → (4,4) PEDESTRIAN ♿✅",
        "(4,4) → (1,4) PEDESTRIAN ♿✅",
        "(4,4) → (4,3) PEDESTRIAN ♿✅",
        "(4,3) → (4,4) PEDESTRIAN ♿✅",
        "(4,3) → (3,3) PEDESTRIAN ♿✅",
        "(3,3) → (4,3) PEDESTRIAN ♿✅",
        "(3,3) → (3,2) PEDESTRIAN ♿✅",
        "(3,2) → (3,3) PEDESTRIAN ♿✅",
        "(3,2) → (4,2) PEDESTRIAN ♿✅",
        "(4,2) → (3,2) PEDESTRIAN ♿✅",
        "(4,2) → (4,1) PEDESTRIAN ♿✅",
        "(4,1) → (4,2) PEDESTRIAN ♿✅",
        // visibility edges linking the concave hole node (4,1) to the outer ring corners,
        // so you can route out of the concave part of the inner ring
        "(0,0) → (4,1) PEDESTRIAN ♿✅",
        "(4,1) → (0,0) PEDESTRIAN ♿✅",
        "(5,5) → (4,1) PEDESTRIAN ♿✅",
        "(4,1) → (5,5) PEDESTRIAN ♿✅"
      );
  }
}
