package org.opentripplanner.graph_builder.module.islandpruning.moduletests;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.street.model.StreetModelForTest.bidirectional;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningEnvironment;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningParameters;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.TransitStopVertex;

/**
 * A disconnected street network that is too small is pruned even if a regular (non-ferry) stop is
 * linked to it: the stop is unlinked from the street network entirely, since it is no better
 * connected than an island without any stops at all.
 */
class StopIslandPrunedTest {

  @Test
  void regularStopIslandIsPruned() {
    // Main street network: a small square of four intersections, large enough to never be
    // considered for pruning.
    var a = intersectionVertex(0, 0);
    var b = intersectionVertex(0, 1);
    var c = intersectionVertex(1, 1);
    var d = intersectionVertex(1, 0);

    // Floating island of two street vertices, with a regular stop linked to one of them.
    var i0 = intersectionVertex(10, 10);
    var i1 = intersectionVertex(10, 11);

    bidirectional(a, b);
    bidirectional(b, c);
    bidirectional(c, d);
    bidirectional(d, a);
    bidirectional(i0, i1);

    var stopVertex = TransitStopVertex.of()
      .withId(id("regular-stop"))
      .withCoordinate(10, 10)
      .build();
    StreetTransitStopLink.createStreetTransitStopLink(i0, stopVertex);
    StreetTransitStopLink.createStreetTransitStopLink(stopVertex, i0);

    // Islands with stops smaller than 3 street vertices are pruned; this island has 2.
    var summarizer = IslandPruningEnvironment.of(a, b, c, d, i0, i1, stopVertex).prune(
      IslandPruningParameters.of()
        .withPruningThresholdIslandWithoutStops(3)
        .withPruningThresholdIslandWithStops(3)
        .withAdaptivePruningFactor(1)
        .build()
    );

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        // main square only: the island's street edges and the stop links are all gone
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
