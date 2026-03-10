package org.opentripplanner.street.linking;

import static org.opentripplanner.street.linking.LinkingDirection.BIDIRECTIONAL;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.Truth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.TraverseModeSet;

/**
 * Tests that the right number of permanent edges are in the graph for the various linking
 * scopes.
 */
class ScopedLinkingTest {

  private static final IntersectionVertex SPLIT = StreetModelFactory.intersectionVertex(0.05, 0.05);

  @Test
  void splitRequestScoped() {
    var env = buildEnv();
    Truth.assertThat(env.graph().listStreetEdges()).hasSize(1);
    var temp = env.linkVertexForRequest(0.05, 0.05);
    Truth.assertThat(env.graph().listStreetEdges()).hasSize(2);
    temp.disposeEdges();
    Truth.assertThat(env.graph().listStreetEdges()).hasSize(1);
  }

  @Test
  void splitPermanently() {
    var env = buildEnv();
    Truth.assertThat(env.graph().listStreetEdges()).hasSize(1);
    TraverseModeSet traverseModes = TraverseModeSet.allModes();
    env
      .linker()
      .linkVertexPermanently(SPLIT, traverseModes, BIDIRECTIONAL, (vertex, streetVertex) ->
        List.of()
      );
    Truth.assertThat(env.graph().listStreetEdges()).hasSize(2);
    env.disposeEdges();
    // edges should stay after disposing
    Truth.assertThat(env.graph().listStreetEdges()).hasSize(2);
  }

  @Test
  void splitRealtime() {
    var env = buildEnv();
    Truth.assertThat(env.graph().listStreetEdges()).hasSize(1);
    TraverseModeSet traverseModes = TraverseModeSet.allModes();
    var temp = env
      .linker()
      .linkVertexForRealTime(SPLIT, traverseModes, BIDIRECTIONAL, (v1, v2) -> List.of());
    Truth.assertThat(env.graph().listStreetEdges()).hasSize(2);
    temp.disposeEdges();
    assertThat(env.graph().listStreetEdges()).hasSize(1);
  }

  private static LinkingEnvironment buildEnv() {
    var v1 = StreetModelFactory.intersectionVertex(0.0, 0.0);
    var v2 = StreetModelFactory.intersectionVertex(0.1, 0.1);

    StreetModelFactory.streetEdge(v1, v2);

    return new LinkingEnvironment(v1, v2);
  }
}
