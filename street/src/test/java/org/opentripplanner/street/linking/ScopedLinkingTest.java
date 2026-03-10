package org.opentripplanner.street.linking;

import static com.google.common.truth.Truth.assertThat;

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
    assertThat(env.graph().listStreetEdges()).hasSize(1);
    var temp = env.linkVertexForRequest(0.05, 0.05);
    assertThat(env.graph().listStreetEdges()).hasSize(2);
    temp.disposeEdges();
    assertThat(env.graph().listStreetEdges()).hasSize(1);
  }

  @Test
  void splitPermanently() {
    var env = buildEnv();
    assertThat(env.graph().listStreetEdges()).hasSize(1);
    TraverseModeSet traverseModes = TraverseModeSet.allModes();
    env.linker().linkVertexBidirectionallyPermanently(SPLIT, traverseModes, (_, _) -> null);
    assertThat(env.graph().listStreetEdges()).hasSize(2);
    env.disposeEdges();
    // edges should stay after disposing
    assertThat(env.graph().listStreetEdges()).hasSize(2);
  }

  @Test
  void splitRealtime() {
    var env = buildEnv();
    assertThat(env.graph().listStreetEdges()).hasSize(1);
    TraverseModeSet traverseModes = TraverseModeSet.allModes();
    var temp = env
      .linker()
      .linkVertexBidirectionallyForRealTime(SPLIT, traverseModes, (_, _) -> null);
    assertThat(env.graph().listStreetEdges()).hasSize(2);
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
