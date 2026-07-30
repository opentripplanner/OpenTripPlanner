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
 * A disconnected street network that only serves a ferry stop is never pruned, no matter how
 * small it is: a ferry stop cannot be reached from elsewhere in the street network, so its
 * connecting street network is retained even below the pruning threshold.
 */
class FerryStopIslandRetainedTest {

  @Test
  void ferryStopIslandIsRetained() {
    // Main street network: a small square of four intersections, large enough to never be
    // considered for pruning.
    var a = intersectionVertex(0, 0);
    var b = intersectionVertex(0, 1);
    var c = intersectionVertex(1, 1);
    var d = intersectionVertex(1, 0);

    // Floating island of two street vertices, with a ferry stop linked to one of them.
    var i0 = intersectionVertex(10, 10);
    var i1 = intersectionVertex(10, 11);

    bidirectional(a, b);
    bidirectional(b, c);
    bidirectional(c, d);
    bidirectional(d, a);
    bidirectional(i0, i1);

    var stopVertex = TransitStopVertex.of()
      .withId(id("ferry-stop"))
      .withCoordinate(10, 10)
      .withIsFerry(true)
      .build();
    StreetTransitStopLink.createStreetTransitStopLink(i0, stopVertex);
    StreetTransitStopLink.createStreetTransitStopLink(stopVertex, i0);

    // Islands with stops smaller than 3 street vertices are normally pruned, but this island has
    // only a ferry stop, so it is exempt.
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
        // main square
        "(0,0) → (0,1) PEDESTRIAN ♿✅",
        "(0,1) → (0,0) PEDESTRIAN ♿✅",
        "(0,1) → (1,1) PEDESTRIAN ♿✅",
        "(1,1) → (0,1) PEDESTRIAN ♿✅",
        "(1,1) → (1,0) PEDESTRIAN ♿✅",
        "(1,0) → (1,1) PEDESTRIAN ♿✅",
        "(1,0) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (1,0) PEDESTRIAN ♿✅",
        // untouched island and its ferry stop links, retained despite being below the threshold
        "(10,10) → (10,11) PEDESTRIAN ♿✅",
        "(10,11) → (10,10) PEDESTRIAN ♿✅",
        "(10,10) linked to (10,10)[F:ferry-stop]",
        "(10,10)[F:ferry-stop] linked to (10,10)"
      );
  }
}
