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
      .build()
      .buildGraph();

    var summarizer = new GraphSummarizer(graph);

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        // connecting ways from outside into two opposite corners
        "(0,0) → (-1,0) PEDESTRIAN ♿✅",
        "(-1,0) → (0,0) PEDESTRIAN ♿✅",
        "(5,5) → (6,5) PEDESTRIAN ♿✅",
        "(6,5) → (5,5) PEDESTRIAN ♿✅",
        // ring edges (boundary of the square)
        "(0,0) → (5,0) PEDESTRIAN ♿✅",
        "(5,0) → (0,0) PEDESTRIAN ♿✅",
        "(5,0) → (5,5) PEDESTRIAN ♿✅",
        "(5,5) → (5,0) PEDESTRIAN ♿✅",
        "(5,5) → (0,5) PEDESTRIAN ♿✅",
        "(0,5) → (5,5) PEDESTRIAN ♿✅",
        "(0,5) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,5) PEDESTRIAN ♿✅",
        // diagonal visibility edge between the two connected corners (shortest crossing)
        "(0,0) → (5,5) PEDESTRIAN ♿✅",
        "(5,5) → (0,0) PEDESTRIAN ♿✅"
      );
  }
}
