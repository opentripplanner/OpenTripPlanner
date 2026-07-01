package org.opentripplanner.street.linking;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetModelFactory;

class ExpandingEnvelopeTest {

  @Test
  void realtimeScopeLinksEdge26mAway() {
    var v1 = intersectionVertex(0.0, 0.0);
    var v2 = intersectionVertex(0.01, 0.0);
    StreetModelFactory.streetEdge(v1, v2);

    var env = new LinkingEnvironment(v1, v2);

    // 26 m ≈ 0.000234 degrees, just beyond REALTIME's first step (25 m) but within the
    // second step (100 m), so the expanding envelope must expand to find the edge
    env.linkVertexForRealTime(0.005, -0.000234);

    assertWithMessage("Inspect graph at %s", env.graph().geoJsonUrl())
      .that(env.graph().summarizeTempEdges())
      .containsExactly("(0,0) → (0.005,0) ALL ♿✅", "(0.005,0) → (0.01,0) ALL ♿✅");

    assertWithMessage("Inspect disposable edges at %s", env.disposable().geojsonUrl())
      .that(env.disposable().summarize())
      .containsExactly(
        "(0,0) → (0.005,0) ALL ♿✅",
        "(0.005,0) → (0.01,0) ALL ♿✅",
        "(0.005,-0.000234) → (0.005,0) ALL"
      );
  }

  @Test
  void realtimeScopeDoesNotLinkEdge101mAway() {
    var v1 = intersectionVertex(0.0, 0.0);
    var v2 = intersectionVertex(0.01, 0.0);
    StreetModelFactory.streetEdge(v1, v2);

    var env = new LinkingEnvironment(v1, v2);

    // 101 m ≈ 0.000907 degrees, beyond REALTIME's maximum radius (100 m across both steps),
    // so the edge must not be linked
    env.linkVertexForRealTime(0.005, -0.000907);

    assertWithMessage("Inspect graph at %s", env.graph().geoJsonUrl())
      .that(env.graph().summarizeTempEdges())
      .isEmpty();

    assertWithMessage("Inspect disposable edges at %s", env.disposable().geojsonUrl())
      .that(env.disposable().summarize())
      .isEmpty();
  }
}
