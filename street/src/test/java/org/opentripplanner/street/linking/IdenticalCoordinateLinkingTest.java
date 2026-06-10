package org.opentripplanner.street.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdFactory.id;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.search.TraverseMode.WALK;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.TraverseModeSet;

/// Tests that linking a vertex with identical coordinates results in the same vertex being re-used
/// and no split edges/vertices being created.
/// It also repeats this with very small variations to ensure that very small differences are being
/// treated equally.
class IdenticalCoordinateLinkingTest {

  @ParameterizedTest
  @ValueSource(doubles = { 0, 0.0000001, 0.0000004, -0.0000001, -0.00000011 })
  void identicalCoordinate(double offset) {
    var v1 = intersectionVertex(0.0, 0.0);
    var v2 = intersectionVertex(0.01, 0.0);

    StreetModelFactory.streetEdge(v1, v2);

    var env = new LinkingEnvironment(v1, v2);

    assertThat(env.graph().listStreetEdges()).hasSize(1);

    var stopVertex = TransitStopVertex.of()
      .withCoordinate(v1.getLat() + offset, v1.getLat() + offset)
      .withId(id("stop"))
      .build();
    env
      .linker()
      .linkVertexPermanently(
        stopVertex,
        new TraverseModeSet(WALK),
        LinkingDirection.BIDIRECTIONAL,
        ((vertex, streetVertex) -> {
          var s = (TransitStopVertex) vertex;
          return List.of(
            StreetTransitStopLink.createStreetTransitStopLink(s, streetVertex),
            StreetTransitStopLink.createStreetTransitStopLink(streetVertex, s)
          );
        })
      );

    assertThat(env.graph().listStreetEdges()).hasSize(1);
    assertThat(env.graph().summarizeEdges()).containsExactly(
      // the street edge
      "(0,0) → (0.01,0) ALL ♿✅",
      // the link edges from/to the stop
      "(0,0) linked to (0,0)[street:stop]",
      "(0,0)[street:stop] linked to (0,0)"
    );
    assertThat(env.graph().summarizeSplitVertices()).isEmpty();
  }
}
