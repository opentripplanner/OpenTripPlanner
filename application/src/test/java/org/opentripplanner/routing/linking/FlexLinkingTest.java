package org.opentripplanner.routing.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.search.TraverseModeSet;

class FlexLinkingTest {

  private static final FeedScopedId AREA_STOP_1 = id("area-stop-1");
  private static final FeedScopedId AREA_STOP_2 = id("area-stop-2");

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

      var env = new LinkingEnvironment(v1, v2);

      env
        .linker()
        .linkVertexBidirectionallyPermanently(
          toBeLinked,
          TraverseModeSet.allModes(),
          StreetModelForTest::streetEdge
        );

      assertThat(env.graph().summarizeSplitVertices()).containsExactly(
        "(0.00055,0.00055)[areaStops=F:area-stop-1,F:area-stop-2]"
      );
    });
  }
}
