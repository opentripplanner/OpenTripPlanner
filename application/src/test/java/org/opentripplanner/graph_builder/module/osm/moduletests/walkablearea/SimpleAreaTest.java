package org.opentripplanner.graph_builder.module.osm.moduletests.walkablearea;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;

class SimpleAreaTest {

  @Test
  void walkableArea() {
    var inside0 = node(0, new WgsCoordinate(0, 0));
    var inside1 = node(2, new WgsCoordinate(5, 5));
    var area = List.of(
      inside0,
      node(1, new WgsCoordinate(0, 5)),
      inside1,
      node(4, new WgsCoordinate(5, 0))
    );

    var outside0 = node(5, new WgsCoordinate(-1, 0));
    var outside1 = node(6, new WgsCoordinate(6, 5));

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(area)
      .addWayFromNodes(outside0, inside0)
      .addWayFromNodes(outside1, inside1)
      .build();

    var graph = new Graph();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withAreaVisibility(true)
      .withMaxAreaNodes(10)
      .build().buildGraph();

    assertFalse(graph.getVertices().isEmpty());
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
