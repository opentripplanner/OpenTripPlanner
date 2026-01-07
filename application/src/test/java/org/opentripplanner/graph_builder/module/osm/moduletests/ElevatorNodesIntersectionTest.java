package org.opentripplanner.graph_builder.module.osm.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.osm.moduletests._support.NodeBuilder.node;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;

class ElevatorNodesIntersectionTest {
  
  @Test
  void elevatorNodeIsAlsoAnIntersection() {
    var n1 = node(1, new WgsCoordinate(1, 1));
    var n2 = node(2, new WgsCoordinate(2, 2));
    var n3 = node(3, new WgsCoordinate(3, 3));

    var n4 = node(4, new WgsCoordinate(4, 4));
    var n5 = node(5, new WgsCoordinate(5, 5));

    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.addTag("level", "-1;-2").addTag("highway", "elevator"), n1, n2, n3)
      .addWayFromNodes(way -> way.addTag("public_transport", "platform"), n4, n2, n5)
      .build();
    var graph = new Graph();


    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    var edges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(edges).hasSize(2);
    for (var edge : edges) {
      assertEquals(1, edge.getLevels());
    }
  }

}
