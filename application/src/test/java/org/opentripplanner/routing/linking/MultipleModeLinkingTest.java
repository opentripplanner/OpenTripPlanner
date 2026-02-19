package org.opentripplanner.routing.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.edge.LinkingDirection.BIDIRECTIONAL;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.routing.graph.DisposableEdgeDataFetcher;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphDataFetcher;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.search.TraverseModeSet;

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

    // link point below all edges, in the middle
    var split = new TemporaryStreetLocation(new Coordinate(-0.0001, 0.005), I18NString.of("split"));

    var g = new Graph();
    for (IntersectionVertex vertex : vertices) {
      g.addVertex(vertex);
    }
    g.index();

    var gf = new GraphDataFetcher(g);

    assertThat(gf.listStreetEdges()).hasSize(3);

    var linker = VertexLinkerTestFactory.of(g);
    var temp = linker.linkVertexForRequest(
      split,
      TraverseModeSet.allModes(),
      BIDIRECTIONAL,
      (v1, v2) ->
        List.of(TemporaryFreeEdge.createTemporaryFreeEdge((TemporaryStreetLocation) v1, v2))
    );
    // vertex is linked to closest walk edge and to the car edge, not to all 3 edges
    assertThat(gf.summarizeTempEdges()).containsExactly(
      "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
      "(0,0.0002) → (0.005,0.0002) CAR ♿✅"
    );

    var tempFetcher = new DisposableEdgeDataFetcher(temp);
    assertThat(tempFetcher.summarize()).containsExactly(
      "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
      "(0.005,0) → (0.01,0) PEDESTRIAN ♿✅",
      "(0,0.0002) → (0.005,0.0002) CAR ♿✅",
      "(0.005,0.0002) → (0.01,0.0002) CAR ♿✅",
      "(0.005,-0.0001) → (0.005,0) ALL",
      "(0.005,-0.0001) → (0.005,0.0002) ALL"
    );
    temp.disposeEdges();

    // after disposing all temporary edges should be gone
    assertThat(gf.summarizeTempEdges()).isEmpty();
    assertThat(tempFetcher.summarize()).isEmpty();
  }
}
