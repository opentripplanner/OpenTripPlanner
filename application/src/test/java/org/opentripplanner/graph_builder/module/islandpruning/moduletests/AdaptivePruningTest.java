package org.opentripplanner.graph_builder.module.islandpruning.moduletests;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.street.model.StreetModelForTest.bidirectional;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningEnvironment;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningParameters;

/**
 * Graph has one main part and two smaller equally sized islands.
 * Island which is close to the main part gets pruned, whereras a distant island
 * is not pruned, because it is considered a real island, not a connectivity error.
 */
class AdaptivePruningTest {

  @Test
  void adaptivePruningTest() {
    // Main street network: a small square of four intersections
    // Coordinates are from the equator where 0.001 degrees is 111 meters.
    var a = intersectionVertex(0, 0);
    var b = intersectionVertex(0, 0.001);
    var c = intersectionVertex(0.001, 0.001);
    var d = intersectionVertex(0.001, 0);

    // Small island over 200 meters to the north
    var e = intersectionVertex(0.003, 0);
    var f = intersectionVertex(0.0035, 0);

    // Another small island about 50 meters to the east
    var g = intersectionVertex(0, 0.0015);
    var h = intersectionVertex(0, 0.002);

    // Main graph
    bidirectional(a, b);
    bidirectional(b, c);
    bidirectional(c, d);
    bidirectional(d, a);

    // Distant island
    bidirectional(e, f);

    // Near island
    bidirectional(g, h);

    var summarizer = IslandPruningEnvironment.of(a, b, c, d, e, f, g, h).prune(
      IslandPruningParameters.of()
        // low pruning threshold which does not apply to any island
        .withPruningThresholdIslandWithoutStops(2)
        // adaptive factor 3 will prune max 3*2 vertex islands
        // note that largest graph is never pruned
        .withAdaptivePruningFactor(3)
        // 100 m distance excludes distant island from adaptive pruning
        .withAdaptivePruningDistance(100)
        .build()
    );

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        // main graph: untouched
        "(0,0) → (0,0.001) PEDESTRIAN ♿✅",
        "(0,0.001) → (0,0) PEDESTRIAN ♿✅",
        "(0,0.001) → (0.001,0.001) PEDESTRIAN ♿✅",
        "(0.001,0.001) → (0,0.001) PEDESTRIAN ♿✅",
        "(0.001,0.001) → (0.001,0) PEDESTRIAN ♿✅",
        "(0.001,0) → (0.001,0.001) PEDESTRIAN ♿✅",
        "(0.001,0) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0.001,0) PEDESTRIAN ♿✅",
        // far island: untouched
        "(0.003,0) → (0.0035,0) PEDESTRIAN ♿✅",
        "(0.0035,0) → (0.003,0) PEDESTRIAN ♿✅"
      );
  }
}
