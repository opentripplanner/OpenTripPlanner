package org.opentripplanner.graph_builder.module.islandpruning.moduletests;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.street.model.StreetModelForTest.bidirectional;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdgeBuilder;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningEnvironment;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningParameters;

/**
 * A dead-end street network that is too small to stand on its own, but is reachable from the main
 * street network via a "no thru traffic" connector (e.g. `foot=destination`), is not removed.
 * Instead its own edges are converted to no-thru-traffic, so it remains reachable as a
 * destination without becoming a shortcut for through traffic.
 */
class DeadEndBecomesNoThruTest {

  @Test
  void deadEndConnectedViaDestinationOnlyWayBecomesNoThru() {
    // Main street network: a small square of four intersections, large enough to never be
    // considered for pruning.
    var a = intersectionVertex(0, 0);
    var b = intersectionVertex(0, 1);
    var c = intersectionVertex(1, 1);
    var d = intersectionVertex(1, 0);

    // Dead-end tail, reachable only via a destination-only ("no thru traffic") connector from
    // the main square.
    var e = intersectionVertex(2, 0);
    var f = intersectionVertex(3, 0);

    bidirectional(a, b);
    bidirectional(b, c);
    bidirectional(c, d);
    bidirectional(d, a);
    bidirectional(e, f);

    // Connector: already destination-only, e.g. tagged `foot=destination` in OSM.
    streetEdgeBuilder(d, e, 1, PEDESTRIAN).withWalkNoThruTraffic(true).buildAndConnect();
    streetEdgeBuilder(e, d, 1, PEDESTRIAN).withWalkNoThruTraffic(true).buildAndConnect();

    // Dead end has 2 street vertices (e, f), which is below the threshold of 3.
    var summarizer = IslandPruningEnvironment.of(a, b, c, d, e, f).prune(
      IslandPruningParameters.of()
        .withPruningThresholdIslandWithoutStops(3)
        .withPruningThresholdIslandWithStops(3)
        .withAdaptivePruningFactor(1)
        .build()
    );

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        // main square: untouched
        "(0,0) → (0,1) PEDESTRIAN ♿✅",
        "(0,1) → (0,0) PEDESTRIAN ♿✅",
        "(0,1) → (1,1) PEDESTRIAN ♿✅",
        "(1,1) → (0,1) PEDESTRIAN ♿✅",
        "(1,1) → (1,0) PEDESTRIAN ♿✅",
        "(1,0) → (1,1) PEDESTRIAN ♿✅",
        "(1,0) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (1,0) PEDESTRIAN ♿✅",
        // connector: already destination-only
        "(1,0) → (2,0) PEDESTRIAN ♿✅ noThru=WALK",
        "(2,0) → (1,0) PEDESTRIAN ♿✅ noThru=WALK",
        // dead-end edge: converted to no-thru-traffic by the pruning module, not removed
        "(2,0) → (3,0) PEDESTRIAN ♿✅ noThru=WALK",
        "(3,0) → (2,0) PEDESTRIAN ♿✅ noThru=WALK"
      );
  }
}
