package org.opentripplanner.routing.linking;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

class MultipleModeLinkingTest {

  @Test
  void multiModeLinking() {
    // test model has 3 parallel horizontal edges, of which uppermost allows car driving
    IntersectionVertex[] vertices = {
      intersectionVertex(0.0, 0.0),
      intersectionVertex(0.01, 0.0),
      intersectionVertex(0.0, 0.0001),
      intersectionVertex(0.01, 0.0001),
      intersectionVertex(0.0, 0.0002),
      intersectionVertex(0.01, 0.0002),
    };

    streetEdge(vertices[0], vertices[1], 0.01, PEDESTRIAN);
    streetEdge(vertices[2], vertices[3], 0.01, PEDESTRIAN);
    streetEdge(vertices[4], vertices[5], 0.01, CAR);

    var env = new LinkingEnvironment(vertices);

    assertThat(env.graph().listStreetEdges()).hasSize(3);

    // link point below all edges, in the middle
    env.linkVertexForRequest(0.005, -0.0001);

    // vertex is linked to closest walk edge and to the car edge, not to all 3 edges
    assertThat(env.graph().summarizeTempEdges()).containsExactly(
      "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
      "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅",
      "(0,0.0002) → (0.005,0.0002) CAR ♿✅",
      "(0.005,0.0002) → (0.01,0.0002) CAR ♿✅"
    );

    // the majority of the temporary edges are in the disposable edge collection
    assertThat(env.disposable().summarize()).containsExactly(
      "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
      "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅",
      "(0,0.0002) → (0.005,0.0002) CAR ♿✅",
      "(0.005,0.0002) → (0.01,0.0002) CAR ♿✅",
      "(0.005,-0.0001) → (0.005,0) ALL",
      "(0.005,-0.0001) → (0.005,0.0002) ALL"
    );
    env.disposeEdges();

    // after disposing all temporary edges should be gone
    assertThat(env.disposable().summarize()).isEmpty();
    assertWithMessage(
      "Graph should not have any temporary edges. Inspect %s",
      env.graph().geoJsonUrl()
    )
      .that(env.graph().summarizeTempEdges())
      .isEmpty();
  }
}
