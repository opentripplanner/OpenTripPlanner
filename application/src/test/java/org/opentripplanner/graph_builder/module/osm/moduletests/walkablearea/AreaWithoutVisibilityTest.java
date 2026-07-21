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
 * Tests the {@code buildWithoutVisibility} code path, activated by {@code withAreaVisibility(false)}.
 *
 * <p>Without visibility computation there is no pruning: every ring segment survives. No
 * cross-area visibility edges are ever added, so the only graph edges are the ring boundary
 * plus any non-area ways that happen to share nodes with the area.
 */
class AreaWithoutVisibilityTest {

  @Test
  void ringEdgesOnlyWhenVisibilityDisabled() {
    var bl = node(0, new WgsCoordinate(0, 0));
    var tl = node(1, new WgsCoordinate(0.001, 0));
    var tr = node(2, new WgsCoordinate(0.001, 0.001));
    var br = node(3, new WgsCoordinate(0, 0.001));
    var area = List.of(bl, tl, tr, br);

    // Footways that touches corner bl and tr
    var outside1 = node(4, new WgsCoordinate(-0.001, 0));
    var outside2 = node(5, new WgsCoordinate(0.002, 0));

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(area)
      .addWayFromNodes(way -> way.withTag("highway", "footway"), outside1, bl)
      .addWayFromNodes(way -> way.withTag("highway", "footway"), outside2, tr)
      .build();

    var graph = new Graph();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withAreaVisibility(false)
      .build()
      .buildGraph();

    var summarizer = new GraphSummarizer(graph);

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        // footway from outside1 to bl
        "(0,0) → (-0.001,0) PEDESTRIAN ♿✅",
        "(-0.001,0) → (0,0) PEDESTRIAN ♿✅",
        // footway from outside2 to tr
        "(0.001,0.001) → (0.002,0) PEDESTRIAN ♿✅",
        "(0.002,0) → (0.001,0.001) PEDESTRIAN ♿✅",
        // ring: all 4 segments × 2 directions, no visibility edges added
        "(0,0) → (0.001,0) PEDESTRIAN ♿✅",
        "(0.001,0) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,0.001) PEDESTRIAN ♿✅",
        "(0,0.001) → (0,0) PEDESTRIAN ♿✅",
        "(0.001,0) → (0.001,0.001) PEDESTRIAN ♿✅",
        "(0.001,0.001) → (0.001,0) PEDESTRIAN ♿✅",
        "(0.001,0.001) → (0,0.001) PEDESTRIAN ♿✅",
        "(0,0.001) → (0.001,0.001) PEDESTRIAN ♿✅"
      );
  }
}
