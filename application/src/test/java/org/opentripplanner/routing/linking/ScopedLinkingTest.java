package org.opentripplanner.routing.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.edge.LinkingDirection.BIDIRECTIONAL;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.TraverseModeSet;

/**
 * Tests that the right number of permanent edges are in the graph for the various linking
 * scopes.
 */
class ScopedLinkingTest {

  private static final IntersectionVertex SPLIT = StreetModelForTest.intersectionVertex(0.05, 0.05);

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
    env
      .linker()
      .linkVertexPermanently(SPLIT, traverseModes, BIDIRECTIONAL, (vertex, streetVertex) ->
        List.of()
      );
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
      .linkVertexForRealTime(SPLIT, traverseModes, BIDIRECTIONAL, (v1, v2) -> List.of());
    assertThat(env.graph().listStreetEdges()).hasSize(2);
    temp.disposeEdges();
    assertThat(env.graph().listStreetEdges()).hasSize(1);
  }

  private static LinkingEnvironment buildEnv() {
    var v1 = StreetModelForTest.intersectionVertex(0.0, 0.0);
    var v2 = StreetModelForTest.intersectionVertex(0.1, 0.1);

    var edge = StreetModelForTest.streetEdge(v1, v2);

    var g = new Graph();
    g.addVertex(v1);
    g.addVertex(v2);
    g.index();
    g.insert(edge, Scope.PERMANENT);

    return new LinkingEnvironment(g);
  }
}
