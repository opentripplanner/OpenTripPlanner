package org.opentripplanner.routing.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.edge.LinkingDirection.BIDIRECTIONAL;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphDataFetcher;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseModeSet;

class FlexLinkingTest {

  public static final FeedScopedId AREA_STOP_1 = id("area-stop-1");
  public static final FeedScopedId AREA_STOP_2 = id("area-stop-2");

  @Test
  void flex() {
    OTPFeature.FlexRouting.testOn(() -> {
      var v1 = intersectionVertex(0.0, 0.0);
      v1.addAreaStops(Set.of(AREA_STOP_1));
      var v2 = intersectionVertex(0.001, 0.001);
      v2.addAreaStops(Set.of(AREA_STOP_2));

      var toBeLinked = intersectionVertex(0.0005, 0.0006);

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
        BIDIRECTIONAL,
        (vertex, streetVertex) ->
          List.of(
            StreetModelForTest.streetEdge((StreetVertex) vertex, streetVertex),
            StreetModelForTest.streetEdge(streetVertex, (StreetVertex) vertex)
          )
      );

      var summary = new GraphDataFetcher(graph).summarizeSplitVertices();

      assertThat(summary).containsExactly(
        "(0.00055,0.00055)[areaStops=F:area-stop-1,F:area-stop-2]"
      );
    });
  }
}
