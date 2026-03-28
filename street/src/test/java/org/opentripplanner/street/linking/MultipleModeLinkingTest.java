package org.opentripplanner.street.linking;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;

class MultipleModeLinkingTest {

  private static final Set<TraverseModeSet> OR_ALL_MODES = Set.of(TraverseModeSet.allModes());
  private static final Set<TraverseModeSet> AND_ALL_MODES = Set.of(
    new TraverseModeSet(TraverseMode.WALK),
    new TraverseModeSet(TraverseMode.BICYCLE),
    new TraverseModeSet(TraverseMode.CAR)
  );

  private static List<Arguments> multiModeLinkingWithSeparateTestCases() {
    return List.of(
      Arguments.of(
        OR_ALL_MODES,
        OR_ALL_MODES,
        List.of("(0,0) → (0.005,0) PEDESTRIAN ♿✅", "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅"),
        List.of(
          "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
          "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅",
          "(0.005,-0.0001) → (0.005,0) PEDESTRIAN",
          "(0.005,0) → (0.005,-0.0001) PEDESTRIAN"
        )
      ),
      Arguments.of(
        AND_ALL_MODES,
        AND_ALL_MODES,
        List.of(
          "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
          "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅",
          "(0,0.0002) → (0.005,0.0002) CAR ♿✅",
          "(0.005,0.0002) → (0.01,0.0002) CAR ♿✅"
        ),
        List.of(
          "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
          "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅",
          "(0,0.0002) → (0.005,0.0002) CAR ♿✅",
          "(0.005,0.0002) → (0.01,0.0002) CAR ♿✅",
          "(0.005,-0.0001) → (0.005,0) PEDESTRIAN",
          "(0.005,0) → (0.005,-0.0001) PEDESTRIAN",
          "(0.005,0.0002) → (0.005,-0.0001) CAR",
          "(0.005,-0.0001) → (0.005,0.0002) CAR"
        )
      ),
      Arguments.of(
        Set.of(new TraverseModeSet(TraverseMode.CAR)),
        Set.of(new TraverseModeSet(TraverseMode.WALK)),
        List.of(
          "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
          "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅",
          "(0,0.0002) → (0.005,0.0002) CAR ♿✅",
          "(0.005,0.0002) → (0.01,0.0002) CAR ♿✅"
        ),
        List.of(
          "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
          "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅",
          "(0,0.0002) → (0.005,0.0002) CAR ♿✅",
          "(0.005,0.0002) → (0.01,0.0002) CAR ♿✅",
          "(0.005,-0.0001) → (0.005,0) PEDESTRIAN",
          "(0.005,0.0002) → (0.005,-0.0001) CAR"
        )
      ),
      Arguments.of(
        AND_ALL_MODES,
        Set.of(),
        List.of("(0,0) → (0.005,0) PEDESTRIAN ♿✅", "(0,0.0002) → (0.005,0.0002) CAR ♿✅"),
        List.of(
          "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
          "(0.005,0.0002) → (0.005,-0.0001) CAR",
          "(0,0.0002) → (0.005,0.0002) CAR ♿✅",
          "(0.005,0) → (0.005,-0.0001) PEDESTRIAN"
        )
      ),
      Arguments.of(
        Set.of(),
        AND_ALL_MODES,
        List.of("(0.005,0.0002) → (0.01,0.0002) CAR ♿✅", "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅"),
        List.of(
          "(0.005,-0.0001) → (0.005,0) PEDESTRIAN",
          "(0.005,-0.0001) → (0.005,0.0002) CAR",
          "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅",
          "(0.005,0.0002) → (0.01,0.0002) CAR ♿✅"
        )
      ),
      Arguments.of(
        OR_ALL_MODES,
        Set.of(),
        List.of("(0,0) → (0.005,0) PEDESTRIAN ♿✅"),
        List.of("(0,0) → (0.005,0) PEDESTRIAN ♿✅", "(0.005,0) → (0.005,-0.0001) PEDESTRIAN")
      ),
      Arguments.of(
        Set.of(),
        OR_ALL_MODES,
        List.of("(0.005,0) → (0.01,0) PEDESTRIAN ♿✅"),
        List.of("(0.005,-0.0001) → (0.005,0) PEDESTRIAN", "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅")
      )
    );
  }

  @ParameterizedTest
  @MethodSource("multiModeLinkingWithSeparateTestCases")
  void multiModeLinkingWithSeparateLinks(
    Set<TraverseModeSet> incoming,
    Set<TraverseModeSet> outgoing,
    List<String> expectedTempEdges,
    List<String> expectedDisposableEdges
  ) {
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

    assertThat(env.graph().listStreetEdges()).hasSize(3);

    // link point below all edges, in the middle
    env.linkVertexForRequest(0.005, -0.0001, incoming, outgoing);

    // vertex is linked to closest walk edge and to the car edge, not to all 3 edges
    assertThat(env.graph().summarizeTempEdges()).containsExactlyElementsIn(expectedTempEdges);

    // the majority of the temporary edges are in the disposable edge collection
    assertThat(env.disposable().summarize()).containsExactlyElementsIn(expectedDisposableEdges);
    env.disposeEdges();

    // after disposing all temporary edges should be gone
    assertCleanUp(env);
  }

  private static List<Arguments> multiModeLinkingWithSameLinksTestCases() {
    return List.of(
      Arguments.of(
        AND_ALL_MODES,
        AND_ALL_MODES,
        List.of("(0,0) → (0.005,0) ALL ♿✅", "(0.005,0) → (0.01,0) ALL ♿✅"),
        List.of(
          "(0,0) → (0.005,0) ALL ♿✅",
          "(0.005,0) → (0.01,0) ALL ♿✅",
          "(0.005,-0.0001) → (0.005,0) ALL",
          "(0.005,0) → (0.005,-0.0001) ALL"
        )
      ),
      Arguments.of(
        OR_ALL_MODES,
        OR_ALL_MODES,
        List.of("(0,0) → (0.005,0) ALL ♿✅", "(0.005,0) → (0.01,0) ALL ♿✅"),
        List.of(
          "(0,0) → (0.005,0) ALL ♿✅",
          "(0.005,0) → (0.01,0) ALL ♿✅",
          "(0.005,-0.0001) → (0.005,0) ALL",
          "(0.005,0) → (0.005,-0.0001) ALL"
        )
      ),
      Arguments.of(
        Set.of(new TraverseModeSet(TraverseMode.CAR)),
        Set.of(new TraverseModeSet(TraverseMode.WALK)),
        List.of("(0,0) → (0.005,0) ALL ♿✅", "(0.005,0) → (0.01,0) ALL ♿✅"),
        List.of(
          "(0,0) → (0.005,0) ALL ♿✅",
          "(0.005,0) → (0.01,0) ALL ♿✅",
          "(0.005,-0.0001) → (0.005,0) PEDESTRIAN",
          "(0.005,0) → (0.005,-0.0001) CAR"
        )
      ),
      Arguments.of(
        AND_ALL_MODES,
        Set.of(),
        List.of("(0,0) → (0.005,0) ALL ♿✅"),
        List.of("(0,0) → (0.005,0) ALL ♿✅", "(0.005,0) → (0.005,-0.0001) ALL")
      ),
      Arguments.of(
        Set.of(),
        AND_ALL_MODES,
        List.of("(0.005,0) → (0.01,0) ALL ♿✅"),
        List.of("(0.005,0) → (0.01,0) ALL ♿✅", "(0.005,-0.0001) → (0.005,0) ALL")
      ),
      Arguments.of(
        OR_ALL_MODES,
        Set.of(),
        List.of("(0,0) → (0.005,0) ALL ♿✅"),
        List.of("(0,0) → (0.005,0) ALL ♿✅", "(0.005,0) → (0.005,-0.0001) ALL")
      ),
      Arguments.of(
        Set.of(),
        OR_ALL_MODES,
        List.of("(0.005,0) → (0.01,0) ALL ♿✅"),
        List.of("(0.005,0) → (0.01,0) ALL ♿✅", "(0.005,-0.0001) → (0.005,0) ALL")
      )
    );
  }

  @ParameterizedTest
  @MethodSource("multiModeLinkingWithSameLinksTestCases")
  void multiModeLinkingWithSameLinks(
    Set<TraverseModeSet> incoming,
    Set<TraverseModeSet> outgoing,
    List<String> expectedTempEdges,
    List<String> expectedDisposableEdges
  ) {
    // test model has 3 parallel horizontal edges, all of them allow everything
    IntersectionVertex[] vertices = {
      StreetModelFactory.intersectionVertex(0.0, 0.0),
      StreetModelFactory.intersectionVertex(0.01, 0.0),
      StreetModelFactory.intersectionVertex(0.0, 0.0001),
      StreetModelFactory.intersectionVertex(0.01, 0.0001),
      StreetModelFactory.intersectionVertex(0.0, 0.0002),
      StreetModelFactory.intersectionVertex(0.01, 0.0002),
    };
    StreetModelFactory.streetEdge(vertices[0], vertices[1], 0.01, ALL);
    StreetModelFactory.streetEdge(vertices[2], vertices[3], 0.01, ALL);
    StreetModelFactory.streetEdge(vertices[4], vertices[5], 0.01, ALL);

    var env = new LinkingEnvironment(vertices);

    assertThat(env.graph().listStreetEdges()).hasSize(3);

    // link point below all edges, in the middle
    env.linkVertexForRequest(0.005, -0.0001, incoming, outgoing);

    // vertex is linked to the closest edge, not to all 3 edges
    assertThat(env.graph().summarizeTempEdges()).containsExactlyElementsIn(expectedTempEdges);

    // the majority of the temporary edges are in the disposable edge collection
    assertThat(env.disposable().summarize()).containsExactlyElementsIn(expectedDisposableEdges);
    env.disposeEdges();

    // after disposing all temporary edges should be gone
    assertCleanUp(env);
  }

  private void assertCleanUp(LinkingEnvironment env) {
    assertThat(env.disposable().summarize()).isEmpty();
    assertWithMessage(
      "Graph should not have any temporary edges. Inspect %s",
      env.graph().geoJsonUrl()
    )
      .that(env.graph().summarizeTempEdges())
      .isEmpty();
  }
}
