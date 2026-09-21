package org.opentripplanner.street.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.linking.LinkingDirection.BIDIRECTIONAL;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.id.FeedScopedIdFactory;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseModeSet;

class FlexLinkingTest {

  private static final FeedScopedId AREA_STOP_1 = FeedScopedIdFactory.id("area-stop-1");
  private static final FeedScopedId AREA_STOP_2 = FeedScopedIdFactory.id("area-stop-2");

  @Test
  void flex() {
    var v1 = StreetModelFactory.intersectionVertex(0.0, 0.0);
    v1.addAreaStops(Set.of(AREA_STOP_1));
    var v2 = StreetModelFactory.intersectionVertex(0.001, 0.001);
    v2.addAreaStops(Set.of(AREA_STOP_2));

    var toBeLinked = StreetModelFactory.intersectionVertex(0.0005, 0.0006);

    assertThat(toBeLinked.areaStops()).isEmpty();

    StreetModelFactory.streetEdge(v1, v2);

    var env = new LinkingEnvironment(v1, v2);

    env
      .linker()
      .linkVertexPermanently(
        toBeLinked,
        TraverseModeSet.allModes(),
        BIDIRECTIONAL,
        (vertex, streetVertex) ->
          List.of(
            StreetModelFactory.streetEdge((StreetVertex) vertex, streetVertex),
            StreetModelFactory.streetEdge(streetVertex, (StreetVertex) vertex)
          )
      );

    assertThat(env.graph().summarizeSplitVertices()).containsExactly(
      "(0.00055,0.00055)[areaStops=street:area-stop-1,street:area-stop-2]"
    );
  }
}
