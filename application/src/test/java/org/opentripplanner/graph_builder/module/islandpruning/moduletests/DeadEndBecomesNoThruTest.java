package org.opentripplanner.graph_builder.module.islandpruning.moduletests;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.buildStreetGraph;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.prune;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.summary.GraphSummarizer;

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
    var a = node(0, new WgsCoordinate(0, 0));
    var b = node(1, new WgsCoordinate(0, 1));
    var c = node(2, new WgsCoordinate(1, 1));
    var d = node(3, new WgsCoordinate(1, 0));

    // Dead-end tail, reachable only via a destination-only ("no thru traffic") connector from
    // the main square.
    var e = node(4, new WgsCoordinate(2, 0));
    var f = node(5, new WgsCoordinate(3, 0));

    var provider = TestOsmProvider.of()
      .addWayFromNodes(a, b)
      .addWayFromNodes(b, c)
      .addWayFromNodes(c, d)
      .addWayFromNodes(d, a)
      .addWayFromNodes(way -> way.withTag("foot", "destination"), d, e)
      .addWayFromNodes(e, f)
      .build();

    var graph = buildStreetGraph(provider);
    // Dead end has 2 street vertices (e, f), which is below the threshold of 3.
    prune(graph, 3, 3, 1, 250);

    var summarizer = new GraphSummarizer(graph);

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
        // connector: already destination-only from the OSM tag
        "(1,0) → (2,0) PEDESTRIAN ♿✅ noThru=WALK",
        "(2,0) → (1,0) PEDESTRIAN ♿✅ noThru=WALK",
        // dead-end edge: converted to no-thru-traffic by the pruning module, not removed
        "(2,0) → (3,0) PEDESTRIAN ♿✅ noThru=WALK",
        "(3,0) → (2,0) PEDESTRIAN ♿✅ noThru=WALK"
      );
  }
}
