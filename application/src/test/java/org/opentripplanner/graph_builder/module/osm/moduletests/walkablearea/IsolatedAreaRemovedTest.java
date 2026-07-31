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
 * Tests the {@code UnconnectedArea} branch in {@code buildWithVisibility}.
 *
 * <p>A convex outer ring whose corners are all non-convex in the CW-ring sense
 * ({@code isNodeConvex} returns false) and has no external way connections produces an empty
 * {@code visibilityVertices} set. The builder then removes every ring edge it just created and
 * records an {@code UnconnectedArea} issue, leaving the graph empty.
 */
class IsolatedAreaRemovedTest {

  @Test
  void isolatedPlatformProducesNoEdges() {
    var bl = node(0, new WgsCoordinate(0, 0));
    var tl = node(1, new WgsCoordinate(0.001, 0));
    var tr = node(2, new WgsCoordinate(0.001, 0.001));
    var br = node(3, new WgsCoordinate(0, 0.001));
    var platform = List.of(bl, tl, tr, br);

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(way -> way.withTag("public_transport", "platform"), platform)
      .build();

    var graph = new Graph();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withAreaVisibility(true)
      .withMaxAreaNodes(50)
      .build()
      .buildGraph();

    var summarizer = new GraphSummarizer(graph);

    assertWithMessage("Expected empty graph — isolated area should be fully removed")
      .that(summarizer.summarizeEdges())
      .isEmpty();
  }
}
