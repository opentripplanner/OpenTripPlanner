package org.opentripplanner.street.linking;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.street.model.StreetModelFactory.areaGroup;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import org.junit.jupiter.api.Test;

/**
 * Tests the forced-link fallback in {@code addPermanentAreaVertex}. The fallback is used when a
 * normal visibility link cannot be created, including when the only visibility vertex overlaps the
 * new vertex and is initially filtered out as a duplicate.
 */
class PermanentAreaVertexLinkingTest {

  @Test
  void outsideAreaVertexLinksToNearestVisibilityVertex() {
    var v0 = intersectionVertex(0, 0);
    var v1 = intersectionVertex(0, 1);
    var v2 = intersectionVertex(1, 1);
    var v3 = intersectionVertex(1, 0);
    var areaGroup = areaGroup(v0, v1, v2, v3).withVisibilityVertices(v0, v2).build();
    var env = LinkingEnvironment.of().addVertices(v0, v1, v2, v3).build();
    var boardingLocation = intersectionVertex(0.1, -0.1);

    assertThat(env.linker().addPermanentAreaVertex(boardingLocation, areaGroup)).isTrue();

    assertWithMessage("Inspect graph at %s", env.graph().geoJsonUrl())
      .that(env.graph().summarizeEdges())
      .containsExactly(
        "(0,0) → (0,1) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(0,1) → (0,0) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(0,1) → (1,1) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(1,1) → (0,1) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(1,1) → (1,0) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(1,0) → (1,1) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(1,0) → (0,0) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(0,0) → (1,0) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(0.1,-0.1) → (0,0) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(0,0) → (0.1,-0.1) PEDESTRIAN_AND_BICYCLE ♿✅"
      );
  }

  @Test
  void vertexAtVisibilityPointStillGetsAreaLink() {
    var v0 = intersectionVertex(0, 0);
    var v1 = intersectionVertex(0, 1);
    var v2 = intersectionVertex(1, 1);
    var v3 = intersectionVertex(1, 0);
    var areaGroup = areaGroup(v0, v1, v2, v3).withVisibilityVertices(v0).build();
    var env = LinkingEnvironment.of().addVertices(v0, v1, v2, v3).build();
    var boardingLocation = intersectionVertex(0, 0);

    assertThat(env.linker().addPermanentAreaVertex(boardingLocation, areaGroup)).isTrue();

    assertWithMessage("Inspect graph at %s", env.graph().geoJsonUrl())
      .that(env.graph().summarizeEdges())
      .containsExactly(
        "(0,0) → (0,1) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(0,1) → (0,0) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(0,1) → (1,1) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(1,1) → (0,1) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(1,1) → (1,0) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(1,0) → (1,1) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(1,0) → (0,0) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(0,0) → (1,0) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(0,0) → (0,0) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(0,0) → (0,0) PEDESTRIAN_AND_BICYCLE ♿✅"
      );
  }
}
