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
 * Checks that a platform (area) connects correctly to a stairway entering from outside.
 * The stair rises from below and meets the bottom-left corner of a square platform.
 */
class PlatformWithStairsTest {

  @Test
  void platformConnectedToStairway() {
    // Square platform: corners listed in order to form a closed polygon
    var bl = node(0, new WgsCoordinate(0, 0));
    var tl = node(1, new WgsCoordinate(5, 0));
    var tr = node(2, new WgsCoordinate(5, 5));
    var br = node(3, new WgsCoordinate(0, 5));

    var platform = List.of(bl, tl, tr, br);

    // Stair rises from outside and terminates inside the platform area.
    // Neither node is shared with the platform polygon.
    var stairBottom = node(4, new WgsCoordinate(-1, 2.5));
    var stairTop = node(5, new WgsCoordinate(2, 2.5));

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(way -> way.withTag("public_transport", "platform"), platform)
      .addWayFromNodes(way -> way.withTag("highway", "steps"), stairBottom, stairTop)
      .build();

    var graph = new Graph();
    var osmModule = OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withAreaVisibility(true)
      .withMaxAreaNodes(10)
      .withPlatformEntriesLinking(true)
      .build();

    osmModule.buildGraph();

    var summarizer = new GraphSummarizer(graph);

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        // stair edges — wheelchair-inaccessible, as expected for steps
        "(-1,2.5) → (2,2.5) PEDESTRIAN ♿❌",
        "(2,2.5) → (-1,2.5) PEDESTRIAN ♿❌",
        // platform ring edges (boundary of the square)
        "(0,0) → (5,0) PEDESTRIAN ♿✅",
        "(5,0) → (0,0) PEDESTRIAN ♿✅",
        "(5,0) → (5,5) PEDESTRIAN ♿✅",
        "(5,5) → (5,0) PEDESTRIAN ♿✅",
        "(5,5) → (0,5) PEDESTRIAN ♿✅",
        "(0,5) → (5,5) PEDESTRIAN ♿✅",
        "(0,5) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,5) PEDESTRIAN ♿✅",
        // stairTop connected to visible platform corners via visibility edges
        "(2,2.5) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (2,2.5) PEDESTRIAN ♿✅",
        "(2,2.5) → (5,5) PEDESTRIAN ♿✅",
        "(5,5) → (2,2.5) PEDESTRIAN ♿✅"
      );
  }
}
