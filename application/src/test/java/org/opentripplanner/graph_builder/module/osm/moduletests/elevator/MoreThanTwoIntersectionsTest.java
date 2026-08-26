package org.opentripplanner.graph_builder.module.osm.moduletests.elevator;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.vertex.ElevatorHopVertex;

public class MoreThanTwoIntersectionsTest {

  /**
   * If an elevator way has three intersection nodes, it is is probably a tagging error.
   * OTP supports it anyway. We need to make sure that the middle intersection node is not created
   * twice, otherwise we get an error during deserialization.
   */
  @Test
  void elevatorWayWithThreeIntersectionNodes() {
    var n1 = node(1, new WgsCoordinate(1, 1));
    var n2 = node(2, new WgsCoordinate(2, 2));
    var n3 = node(3, new WgsCoordinate(3, 3));
    var n4 = node(4, new WgsCoordinate(4, 4));
    var n5 = node(5, new WgsCoordinate(5, 5));

    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.withTag("highway", "elevator"), n1, n2, n3)
      .addWayFromNodes(way -> way.withTag("public_transport", "platform"), n4, n2, n5)
      .build();
    var graph = new Graph();

    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    var elevatorHopEdges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(elevatorHopEdges).hasSize(4);
    var elevatorHopVertices = graph
      .getVerticesOfType(ElevatorHopVertex.class)
      .stream()
      .map(vertex -> vertex.getLabelString());
    assertThat(elevatorHopVertices).containsNoDuplicates();
  }
}
