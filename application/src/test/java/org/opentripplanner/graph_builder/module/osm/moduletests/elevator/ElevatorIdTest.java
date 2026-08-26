package org.opentripplanner.graph_builder.module.osm.moduletests.elevator;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.CompoundRefTagGroup;
import org.opentripplanner.osm.model.NodeBuilder;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;

public class ElevatorIdTest {

  @Test
  void testElevatorRefTagsOnNode() {
    var node0 = node(0, new WgsCoordinate(0, 0));
    var node1 = node(1, new WgsCoordinate(2, 0));
    var elevatorNode = NodeBuilder.of(2, new WgsCoordinate(1, 0))
      .withTag("highway", "elevator")
      .withTag("ref", "12345")
      .build();
    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.withTag("level", "1"), node0, elevatorNode)
      .addWayFromNodes(way -> way.withTag("level", "2"), node1, elevatorNode)
      .build();
    var graph = new Graph();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withElevatorRefTags(List.of(CompoundRefTagGroup.of("ref")))
      .build()
      .buildGraph();

    var edges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(edges).hasSize(2);
    for (var edge : edges) {
      assertEquals(Optional.of("12345"), edge.id());
    }
  }

  @Test
  void testElevatorRefTagsOnWay() {
    var n1 = node(1, new WgsCoordinate(0, 1));
    var n2 = node(2, new WgsCoordinate(0, 2));

    var elevatorWay = OsmWay.of()
      .withId(1)
      .withTag("highway", "elevator")
      .withTag("ref", "12345")
      .addNodeRef(1, 2)
      .build();

    var provider = new TestOsmProvider(List.of(), List.of(elevatorWay), List.of(n1, n2));
    var graph = new Graph();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withElevatorRefTags(List.of(CompoundRefTagGroup.of("ref")))
      .build()
      .buildGraph();

    var edges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(edges).hasSize(2);
    for (var edge : edges) {
      assertEquals(Optional.of("12345"), edge.id());
    }
  }

  @Test
  void testElevatorRefTagsEmptyWhenNoGroupResolves() {
    var n1 = node(1, new WgsCoordinate(0, 1));
    var n2 = node(2, new WgsCoordinate(0, 2));

    var elevatorWay = OsmWay.of()
      .withId(1)
      .withTag("highway", "elevator")
      .withTag("ref", "12345")
      .addNodeRef(1, 2)
      .build();

    var provider = new TestOsmProvider(List.of(), List.of(elevatorWay), List.of(n1, n2));
    var graph = new Graph();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withElevatorRefTags(List.of(CompoundRefTagGroup.of("manufacturer", "ref")))
      .build()
      .buildGraph();

    var edges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(edges).hasSize(2);
    for (var edge : edges) {
      assertEquals(Optional.empty(), edge.id());
    }
  }
}
