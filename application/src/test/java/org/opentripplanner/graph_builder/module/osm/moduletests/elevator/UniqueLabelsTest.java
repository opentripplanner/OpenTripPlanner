package org.opentripplanner.graph_builder.module.osm.moduletests.elevator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.NodeBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;

class UniqueLabelsTest {

  @ParameterizedTest
  @CsvSource(
    value = {
      "1, 2, 3, 4",
      "1, 1, 1, 1",
      "0, 1, 1, null",
      "null, null, 1, null",
      "null, null, null, null",
    },
    nullValues = "null"
  )
  void testOsmElevatorNodeUniqueLabels(String level1, String ref1, String level2, String ref2) {
    var n1 = node(1, new WgsCoordinate(0, 1));
    var n2 = node(2, new WgsCoordinate(0, 2));
    var elevatorNode = NodeBuilder.of(3, new WgsCoordinate(0, 3))
      .withTag("highway", "elevator")
      .build();
    var provider = TestOsmProvider.of()
      .addWayFromNodes(
        way -> {
          way.withTag("level", level1);
          way.withTag("level:ref", ref1);
        },
        n1,
        elevatorNode
      )
      .addWayFromNodes(
        way -> {
          way.withTag("level", level2);
          way.withTag("level:ref", ref2);
        },
        elevatorNode,
        n2
      )
      .build();
    var graph = new Graph();

    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    assertEquals(
      graph.getVertices().size(),
      graph
        .getVertices()
        .stream()
        .map(vertex -> vertex.getLabel())
        .distinct()
        .count()
    );
  }
}
