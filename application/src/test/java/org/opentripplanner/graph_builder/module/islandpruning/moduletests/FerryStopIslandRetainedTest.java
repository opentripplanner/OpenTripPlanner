package org.opentripplanner.graph_builder.module.islandpruning.moduletests;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.buildStreetGraph;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.prune;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningParameters;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.summary.GraphSummarizer;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VertexLabel;

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
    var a = node(0, new WgsCoordinate(0, 0));
    var b = node(1, new WgsCoordinate(0, 1));
    var c = node(2, new WgsCoordinate(1, 1));
    var d = node(3, new WgsCoordinate(1, 0));

    // Floating island of two street vertices, with a ferry stop linked to one of them.
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
      .withId(id("ferry-stop"))
      .withCoordinate(10, 10)
      .withIsFerry(true)
      .build();
    graph.addVertex(stopVertex);
    var streetVertex = (StreetVertex) graph.getVertex(VertexLabel.osm(i0.getId()));
    StreetTransitStopLink.createStreetTransitStopLink(streetVertex, stopVertex);
    StreetTransitStopLink.createStreetTransitStopLink(stopVertex, streetVertex);

    // Islands with stops smaller than 3 street vertices are normally pruned, but this island has
    // only a ferry stop, so it is exempt.
    prune(
      graph,
      IslandPruningParameters.of()
        .withPruningThresholdIslandWithoutStops(3)
        .withPruningThresholdIslandWithStops(3)
        .withAdaptivePruningFactor(1)
        .build()
    );

    var summarizer = new GraphSummarizer(graph);

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
