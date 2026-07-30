package org.opentripplanner.graph_builder.module.islandpruning.moduletests;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.buildStreetGraph;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.prune;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.summary.GraphSummarizer;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VertexLabel;

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
    var a = node(0, new WgsCoordinate(0, 0));
    var b = node(1, new WgsCoordinate(0, 1));
    var c = node(2, new WgsCoordinate(1, 1));
    var d = node(3, new WgsCoordinate(1, 0));

    // Floating island of two street vertices, with a regular stop linked to one of them.
    var i0 = node(4, new WgsCoordinate(10, 10));
    var i1 = node(5, new WgsCoordinate(10, 11));

    var provider = TestOsmProvider.of()
      .addWayFromNodes(a, b)
      .addWayFromNodes(b, c)
      .addWayFromNodes(c, d)
      .addWayFromNodes(d, a)
      .addWayFromNodes(i0, i1)
      .build();

    var graph = buildStreetGraph(provider);

    var stopVertex = TransitStopVertex.of()
      .withId(id("regular-stop"))
      .withCoordinate(10, 10)
      .build();
    graph.addVertex(stopVertex);
    var streetVertex = (StreetVertex) graph.getVertex(VertexLabel.osm(i0.getId()));
    StreetTransitStopLink.createStreetTransitStopLink(streetVertex, stopVertex);
    StreetTransitStopLink.createStreetTransitStopLink(stopVertex, streetVertex);

    // Islands with stops smaller than 3 street vertices are pruned; this island has 2.
    prune(graph, 3, 3, 1, 250);

    var summarizer = new GraphSummarizer(graph);

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
