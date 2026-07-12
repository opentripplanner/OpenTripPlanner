package org.opentripplanner.street.linking;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import org.junit.jupiter.api.Test;

class PermanentAreaVertexLinkingTest {

  private static final String FORWARD_LINK = "(0.1,-0.1) → (0,0) PEDESTRIAN_AND_BICYCLE ♿✅";
  private static final String REVERSE_LINK = "(0,0) → (0.1,-0.1) PEDESTRIAN_AND_BICYCLE ♿✅";
  private static final String OVERLAPPING_LINK = "(0,0) → (0,0) PEDESTRIAN_AND_BICYCLE ♿✅";

  @Test
  void outsideAreaVertexLinksToNearestVisibilityVertex() {
    var v0 = intersectionVertex("0", 0, 0);
    var v1 = intersectionVertex("1", 0, 1);
    var v2 = intersectionVertex("2", 1, 1);
    var v3 = intersectionVertex("3", 1, 0);
    var env = new LinkingEnvironment(v0, v1, v2, v3);
    var areaGroup = env.areaGroup(v0, v1, v2, v3).withVisibilityVertices(v0, v2).build();
    var boardingLocation = intersectionVertex("boarding-location", 0.1, -0.1);

    assertThat(env.linker().addPermanentAreaVertex(boardingLocation, areaGroup)).isTrue();

    assertWithMessage("Inspect graph at %s", env.graph().geoJsonUrl())
      .that(env.graph().summarizeEdges())
      .containsAtLeast(FORWARD_LINK, REVERSE_LINK);
  }

  @Test
  void vertexAtVisibilityPointStillGetsAreaLink() {
    var v0 = intersectionVertex("0", 0, 0);
    var v1 = intersectionVertex("1", 0, 1);
    var v2 = intersectionVertex("2", 1, 1);
    var v3 = intersectionVertex("3", 1, 0);
    var env = new LinkingEnvironment(v0, v1, v2, v3);
    var areaGroup = env.areaGroup(v0, v1, v2, v3).withVisibilityVertices(v0).build();
    var boardingLocation = intersectionVertex("boarding-location", 0, 0);

    assertThat(env.linker().addPermanentAreaVertex(boardingLocation, areaGroup)).isTrue();

    assertWithMessage("Inspect graph at %s", env.graph().geoJsonUrl())
      .that(env.graph().summarizeEdges())
      .containsAtLeast(OVERLAPPING_LINK, OVERLAPPING_LINK);
  }
}
