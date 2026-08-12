package org.opentripplanner.graph_builder.module.islandpruning.moduletests;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.street.model.StreetModelForTest.bidirectional;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningEnvironment;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningParameters;

/**
 * A disconnected street network is only pruned if its size, measured in street vertices, is
 * strictly below the configured threshold. A disconnected network that meets the threshold is
 * left untouched, even though it has no connection to the rest of the graph.
 */
class IslandAboveThresholdRetainedTest {

  @Test
  void islandAtThresholdIsRetained() {
    // Main street network: a small square of four intersections, large enough to never be
    // considered for pruning.
    var a = intersectionVertex(0, 0);
    var b = intersectionVertex(0, 1);
    var c = intersectionVertex(1, 1);
    var d = intersectionVertex(1, 0);

    // Disconnected island of exactly three street vertices: at the pruning threshold, so it is
    // retained.
    var i0 = intersectionVertex(10, 10);
    var i1 = intersectionVertex(10, 11);
    var i2 = intersectionVertex(10, 12);

    bidirectional(a, b);
    bidirectional(b, c);
    bidirectional(c, d);
    bidirectional(d, a);
    bidirectional(i0, i1);
    bidirectional(i1, i2);

    // Islands without stops smaller than 3 street vertices are pruned; this island has exactly 3.
    var summarizer = IslandPruningEnvironment.of(a, b, c, d, i0, i1, i2).prune(
      IslandPruningParameters.of()
        .withPruningThresholdIslandWithoutStops(3)
        .withPruningThresholdIslandWithStops(3)
        .withAdaptivePruningFactor(1)
        .build()
    );

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        // main square
        "(0,0) → (0,1) PEDESTRIAN ♿✅",
        "(0,1) → (0,0) PEDESTRIAN ♿✅",
        "(0,1) → (1,1) PEDESTRIAN ♿✅",
        "(1,1) → (0,1) PEDESTRIAN ♿✅",
        "(1,1) → (1,0) PEDESTRIAN ♿✅",
        "(1,0) → (1,1) PEDESTRIAN ♿✅",
        "(1,0) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (1,0) PEDESTRIAN ♿✅",
        // untouched island, retained because it is not below the threshold
        "(10,10) → (10,11) PEDESTRIAN ♿✅",
        "(10,11) → (10,10) PEDESTRIAN ♿✅",
        "(10,11) → (10,12) PEDESTRIAN ♿✅",
        "(10,12) → (10,11) PEDESTRIAN ♿✅"
      );
  }
}
