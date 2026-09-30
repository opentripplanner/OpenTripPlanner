package org.opentripplanner.graph_builder.module.islandpruning.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetModelForTest.areaEdge;
import static org.opentripplanner.street.model.StreetModelForTest.bidirectional;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningEnvironment;
import org.opentripplanner.graph_builder.module.islandpruning.IslandPruningParameters;
import org.opentripplanner.street.model.edge.AreaGroup;

/**
 * After pruning, street vertices without any edges are removed from the graph. Visibility vertices
 * of a walkable area are exempt even when edgeless, since the area still references them and
 * graph serialization breaks if they are missing.
 */
class VisibilityVertexRetainedTest {

  @Test
  void edgelessVisibilityVertexIsRetained() {
    // Main street network: a small square of four intersections, large enough to never be
    // considered for pruning. The side a-b runs along a walkable area.
    var a = intersectionVertex(0, 0);
    var b = intersectionVertex(0, 1);
    var c = intersectionVertex(1, 1);
    var d = intersectionVertex(1, 0);

    // A visibility vertex of the area which has no edges of its own.
    var visibilityVertex = intersectionVertex(0.5, 0.5);
    // An unrelated vertex without any edges.
    var orphan = intersectionVertex(10, 10);

    var area = AreaGroup.of(null).withVisibilityVertices(Set.of(visibilityVertex)).build();
    areaEdge(a, b, area, false);
    areaEdge(b, a, area, true);
    bidirectional(b, c);
    bidirectional(c, d);
    bidirectional(d, a);

    var summarizer = IslandPruningEnvironment.of(a, b, c, d, visibilityVertex, orphan).prune(
      IslandPruningParameters.DEFAULTS
    );

    assertThat(summarizer.graph().getVertices()).containsExactly(a, b, c, d, visibilityVertex);
  }
}
