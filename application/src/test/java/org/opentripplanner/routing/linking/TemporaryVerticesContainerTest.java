package org.opentripplanner.routing.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;

public class TemporaryVerticesContainerTest {

  @Test
  void temporaryChangesRemovedOnClose() {
    var graph = new Graph();
    var intersectionVertexA = StreetModelForTest.intersectionVertex(new Coordinate(0, 0));
    var intersectionVertexB = StreetModelForTest.intersectionVertex(new Coordinate(0, 1));
    var intersectionVertexC = StreetModelForTest.intersectionVertex(new Coordinate(1, 0));
    StreetModelForTest.streetEdge(
      intersectionVertexA,
      intersectionVertexB,
      StreetTraversalPermission.PEDESTRIAN
    );
    var secondEdge = StreetModelForTest.streetEdge(
      intersectionVertexB,
      intersectionVertexC,
      StreetTraversalPermission.PEDESTRIAN
    );
    graph.addVertex(intersectionVertexA);
    graph.addVertex(intersectionVertexB);
    graph.addVertex(intersectionVertexC);
    graph.index();
    try (var container = new TemporaryVerticesContainer()) {
      var vertexLinker = VertexLinkerTestFactory.of(graph);
      var temporaryLocation = new TemporaryStreetLocation(
        new Coordinate(0.5, 0.5),
        new NonLocalizedString("Temp location")
      );
      var collection = vertexLinker.linkVertexForRequest(
        temporaryLocation,
        new TraverseModeSet(TraverseMode.WALK),
        LinkingDirection.OUTGOING,
        (vertex, streetVertex) ->
          List.of(TemporaryFreeEdge.createTemporaryFreeEdge((TemporaryVertex) vertex, streetVertex))
      );
      container.addEdgeCollection(collection);
      // There should be vertices from B -> C and B -> to split edge
      assertThat(intersectionVertexB.getOutgoing()).hasSize(2);
      // There should not be other temporary changes
      assertThat(intersectionVertexA.getOutgoing()).hasSize(1);
      assertThat(intersectionVertexA.getIncoming()).hasSize(0);
      assertThat(intersectionVertexB.getIncoming()).hasSize(1);
      assertThat(intersectionVertexC.getIncoming()).hasSize(1);
      assertThat(intersectionVertexC.getOutgoing()).hasSize(0);
    }
    // After container is closed, only vertex from B -> C should remain
    assertThat(intersectionVertexB.getOutgoing()).hasSize(1);
    var edge = intersectionVertexB.getOutgoing().iterator().next();
    assertEquals(secondEdge, edge);
  }
}
