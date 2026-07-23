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
 * Tests that {@code wheelchair=no} on an area propagates to all generated ring edges.
 *
 * <p>The accessibility flag is set in {@code createSegments} from
 * {@code parent.isWheelchairAccessible()}, so every AreaEdge produced for a
 * {@code wheelchair=no} area should carry {@code ♿❌}.
 *
 * <p>Visibility is disabled so the expected edges are simply the four ring segments.
 * The connecting footway is accessible and retains {@code ♿✅}.
 */
class WheelchairInaccessibleAreaTest {

  @Test
  void areaEdgesInheritWheelchairInaccessibility() {
    var bl = node(0, new WgsCoordinate(0, 0));
    var tl = node(1, new WgsCoordinate(0.001, 0));
    var tr = node(2, new WgsCoordinate(0.001, 0.001));
    var br = node(3, new WgsCoordinate(0, 0.001));
    var area = List.of(bl, tl, tr, br);

    var outside = node(4, new WgsCoordinate(-0.001, 0));

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(
        way -> way.withTag("highway", "pedestrian").withTag("wheelchair", "no"),
        area
      )
      .addWayFromNodes(way -> way.withTag("highway", "footway"), outside, bl)
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
        // connecting footway stays accessible
        "(0,0) → (-0.001,0) PEDESTRIAN ♿✅",
        "(-0.001,0) → (0,0) PEDESTRIAN ♿✅",
        // all ring edges inherit wheelchair=no from the area
        "(0,0) → (0.001,0) PEDESTRIAN ♿❌",
        "(0.001,0) → (0,0) PEDESTRIAN ♿❌",
        "(0,0) → (0,0.001) PEDESTRIAN ♿❌",
        "(0,0.001) → (0,0) PEDESTRIAN ♿❌",
        "(0.001,0) → (0.001,0.001) PEDESTRIAN ♿❌",
        "(0.001,0.001) → (0.001,0) PEDESTRIAN ♿❌",
        "(0.001,0.001) → (0,0.001) PEDESTRIAN ♿❌",
        "(0,0.001) → (0.001,0.001) PEDESTRIAN ♿❌"
      );
  }
}
