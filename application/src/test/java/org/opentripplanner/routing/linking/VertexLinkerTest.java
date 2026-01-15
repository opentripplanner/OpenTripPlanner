package org.opentripplanner.routing.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseModeSet;

class VertexLinkerTest {

  public static final FeedScopedId AREA_STOP_1 = id("area-stop-1");
  public static final FeedScopedId AREA_STOP_2 = id("area-stop-2");

  @Test
  void flex() {
    OTPFeature.FlexRouting.testOn(() -> {
      var v1 = StreetModelForTest.intersectionVertex(0.0, 0.0);
      v1.addAreaStops(Set.of(AREA_STOP_1));
      var v2 = StreetModelForTest.intersectionVertex(0.001, 0.001);
      v2.addAreaStops(Set.of(AREA_STOP_2));

      var toBeLinked = StreetModelForTest.intersectionVertex(0.0005, 0.0006);

      assertThat(toBeLinked.areaStops()).isEmpty();

      StreetModelForTest.streetEdge(v1, v2);

      var graph = new Graph();

      graph.addVertex(v1);
      graph.addVertex(v2);
      graph.index();

      var linker = VertexLinkerTestFactory.of(graph);

      linker.linkVertexPermanently(
        toBeLinked,
        TraverseModeSet.allModes(),
        LinkingDirection.BIDIRECTIONAL,
        (vertex, streetVertex) ->
          List.of(
            StreetModelForTest.streetEdge((StreetVertex) vertex, streetVertex),
            StreetModelForTest.streetEdge(streetVertex, (StreetVertex) vertex)
          )
      );

      var splitterVertices = graph.getVerticesOfType(SplitterVertex.class);
      assertThat(splitterVertices).hasSize(1);
      var splitter = splitterVertices.getFirst();

      assertThat(splitter.areaStops()).containsExactly(AREA_STOP_1, AREA_STOP_2);
    });
  }
}
