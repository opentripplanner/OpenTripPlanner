package org.opentripplanner.street.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import com.google.common.truth.Truth;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

class MultipleModeLinkingTest {

  @Test
  void multiModeLinking() {
    // test model has 3 parallel horizontal edges, of which uppermost allows car driving
    IntersectionVertex[] vertices = {
      StreetModelFactory.intersectionVertex(0.0, 0.0),
      StreetModelFactory.intersectionVertex(0.01, 0.0),
      StreetModelFactory.intersectionVertex(0.0, 0.0001),
      StreetModelFactory.intersectionVertex(0.01, 0.0001),
      StreetModelFactory.intersectionVertex(0.0, 0.0002),
      StreetModelFactory.intersectionVertex(0.01, 0.0002),
    };

    StreetModelFactory.streetEdge(vertices[0], vertices[1], 0.01, PEDESTRIAN);
    StreetModelFactory.streetEdge(vertices[2], vertices[3], 0.01, PEDESTRIAN);
    StreetModelFactory.streetEdge(vertices[4], vertices[5], 0.01, CAR);

    var env = new LinkingEnvironment(vertices);

    Truth.assertThat(env.graph().listStreetEdges()).hasSize(3);

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
    Truth.assertThat(env.disposable().summarize()).containsExactly(
      "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
      "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅",
      "(0,0.0002) → (0.005,0.0002) CAR ♿✅",
      "(0.005,0.0002) → (0.01,0.0002) CAR ♿✅",
      "(0.005,-0.0001) → (0.005,0) ALL",
      "(0.005,-0.0001) → (0.005,0.0002) ALL"
    );
    env.disposeEdges();

    // after disposing all temporary edges should be gone
    Truth.assertThat(env.disposable().summarize()).isEmpty();
    Truth.assertWithMessage(
      "Graph should not have any temporary edges. Inspect %s",
      env.graph().geoJsonUrl()
    )
      .that(env.graph().summarizeTempEdges())
      .isEmpty();
  }
}
