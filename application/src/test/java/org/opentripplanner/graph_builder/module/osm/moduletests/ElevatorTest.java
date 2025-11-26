package org.opentripplanner.graph_builder.module.osm.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.osm.moduletests._support.NodeBuilder.node;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;

class ElevatorTest {

  @Test
  void testDuration() {
    var way = new OsmWay();
    way.addTag("duration", "00:01:02");
    way.addTag("highway", "elevator");
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
    var node = node(2, new WgsCoordinate(1, 0));
    node.addTag("duration", "00:01:02");
    node.addTag("highway", "elevator");
    node.addTag("level", "1;2");
    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.addTag("level", "1"), node0, node)
      .addWayFromNodes(way -> way.addTag("level", "2"), node1, node)
      .build();
    var graph = new Graph();

    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    var edges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(edges).hasSize(2);
    for (var edge : edges) {
      assertThat(edge.getTravelTime()).hasValue(Duration.ofSeconds(62));
    }
  }

  @ParameterizedTest
  @CsvSource(
    value = {
      "1, 2, 3, 4", "1, 1, 1, 1", "0, 1, 1, null", "null, null, 1, null", "null, null, null, null",
    },
    nullValues = "null"
  )
  void testOsmElevatorNodeWithLevels(String level1, String ref1, String level2, String ref2) {
    var n1 = new OsmNode(0, 0);
    n1.setId(1);
    var n2 = new OsmNode(0, 1);
    n2.addTag("highway", "elevator");
    n2.setId(2);
    var n3 = new OsmNode(0, 2);
    n3.setId(3);

    var way1 = new OsmWay();
    way1.setId(1);
    way1.addTag("highway", "corridor");
    way1.addTag("level", level1);
    way1.addTag("level:ref", ref1);
    way1.addNodeRef(1);
    way1.addNodeRef(2);

    var way2 = new OsmWay();
    way2.setId(2);
    way2.addTag("highway", "corridor");
    way2.addTag("level", level2);
    way2.addTag("level:ref", ref2);
    way2.addNodeRef(2);
    way2.addNodeRef(3);

    var osmProvider = new TestOsmProvider(List.of(), List.of(way1, way2), List.of(n1, n2, n3));
    var osmDb = new OsmDatabase(DataImportIssueStore.NOOP);
    osmProvider.readOsm(osmDb);
    var graph = new Graph();
    var osmModule = OsmModuleTestFactory.of(osmProvider).withGraph(graph).builder().build();
    osmModule.buildGraph();

    assertEquals(
      graph.getVertices().size(),
      graph.getVertices().stream().map(vertex -> vertex.getLabel()).distinct().count()
    );
  }
}
