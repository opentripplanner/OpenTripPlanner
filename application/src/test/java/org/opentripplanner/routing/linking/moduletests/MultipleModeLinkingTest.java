package org.opentripplanner.routing.linking.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.routing.linking.Scope.PERMANENT;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.edge.LinkingDirection.BIDIRECTIONAL;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphDataFetcher;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
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

    var walkEdge1 = StreetModelForTest.streetEdge(
      vertices[0],
      vertices[1],
      0.01,
      PEDESTRIAN
    );
    var walkEdge2 = StreetModelForTest.streetEdge(
      vertices[2],
      vertices[3],
      0.01,
      PEDESTRIAN
    );
    var carEdge = StreetModelForTest.streetEdge(
      vertices[4],
      vertices[5],
      0.01,
      CAR
    );

    // link point below all edges, in the middle
    var split = intersectionVertex(0.005, -0.0001);

    var g = new Graph();
    for (IntersectionVertex vertex : vertices) {
      g.addVertex(vertex);
    }
    g.index();
    g.insert(walkEdge1, PERMANENT);
    g.insert(walkEdge2, PERMANENT);
    g.insert(carEdge, PERMANENT);

    var gf = new GraphDataFetcher(g);

    assertThat(gf.listStreetEdges()).hasSize(3);

    var linker = VertexLinkerTestFactory.of(g);
    var temp = linker.linkVertexForRequest(
      split,
      TraverseModeSet.allModes(),
      BIDIRECTIONAL,
      (v1, v2) -> List.of()
    );
    // vertex is linked to closest walk edge and to the car edge, not to all 3 edges
    assertThat(gf.summarizeLinks()).containsExactly(
      "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
      "(0,0.0002) → (0.005,0.0002) CAR ♿✅"
    );
    temp.disposeEdges();
    assertThat(gf.summarizeLinks()).isEmpty();
  }
}
