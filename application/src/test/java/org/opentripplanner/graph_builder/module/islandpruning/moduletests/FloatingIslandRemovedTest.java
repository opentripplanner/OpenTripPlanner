package org.opentripplanner.graph_builder.module.islandpruning.moduletests;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.street.model.StreetModelForTest.bidirectional;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningEnvironment;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningParameters;

/**
 * A small street network that has no connection at all to the main street network is entirely
 * removed from the graph, since it is too small to be a plausible independent street network.
 */
class FloatingIslandRemovedTest {

  @Test
  void floatingIslandIsRemoved() {
    // Main street network: a small square of four intersections, large enough to never be
    // considered for pruning.
    var a = intersectionVertex(0, 0);
    var b = intersectionVertex(0, 1);
    var c = intersectionVertex(1, 1);
    var d = intersectionVertex(1, 0);

    // Floating island: two vertices, completely disconnected from the main street network.
    var i0 = intersectionVertex(10, 10);
    var i1 = intersectionVertex(10, 11);

    bidirectional(a, b);
    bidirectional(b, c);
    bidirectional(c, d);
    bidirectional(d, a);
    bidirectional(i0, i1);

    // Islands without stops smaller than 3 street vertices are pruned.
    var summarizer = IslandPruningEnvironment.of(a, b, c, d, i0, i1).prune(
      IslandPruningParameters.of()
        .withPruningThresholdIslandWithoutStops(3)
        .withPruningThresholdIslandWithStops(3)
        .withAdaptivePruningFactor(1)
        .build()
    );

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        "(0,0) → (0,1) PEDESTRIAN ♿✅",
        "(0,1) → (0,0) PEDESTRIAN ♿✅",
        "(0,1) → (1,1) PEDESTRIAN ♿✅",
        "(1,1) → (0,1) PEDESTRIAN ♿✅",
        "(1,1) → (1,0) PEDESTRIAN ♿✅",
        "(1,0) → (1,1) PEDESTRIAN ♿✅",
        "(1,0) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (1,0) PEDESTRIAN ♿✅"
      );
  }
}
