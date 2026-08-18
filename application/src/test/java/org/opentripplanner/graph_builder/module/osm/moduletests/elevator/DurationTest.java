package org.opentripplanner.graph_builder.module.osm.moduletests.elevator;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.NodeBuilder;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;

class DurationTest {

  @Test
  void testDuration() {
    var way = OsmWay.of().withTag("duration", "00:01:02").withTag("highway", "elevator").build();
    var provider = TestOsmProvider.of().addWay(way).build();
    var graph = new Graph();
    var osmModule = OsmModuleTestFactory.of(provider).withGraph(graph).builder().build();

    osmModule.buildGraph();

    var edges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(edges).hasSize(2);
    for (var edge : edges) {
      assertThat(edge.getTravelTime()).hasValue(Duration.ofSeconds(62));
    }
  }

  @Test
  void testMultilevelNodeDuration() {
    var node0 = node(0, new WgsCoordinate(0, 0));
    var node1 = node(1, new WgsCoordinate(2, 0));
    var elevatorNode = NodeBuilder.of(2, new WgsCoordinate(1, 0))
      .withTag("duration", "00:01:02")
      .withTag("highway", "elevator")
      .withTag("level", "1;2")
      .build();
    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.withTag("level", "1"), node0, elevatorNode)
      .addWayFromNodes(way -> way.withTag("level", "2"), node1, elevatorNode)
      .build();
    var graph = new Graph();

    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    var edges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(edges).hasSize(2);
    for (var edge : edges) {
      assertThat(edge.getTravelTime()).hasValue(Duration.ofSeconds(62));
    }
  }
}
