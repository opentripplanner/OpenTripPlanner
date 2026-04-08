package org.opentripplanner.graph_builder.module.osm.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.issues.FewerThanTwoIntersectionNodesInElevatorWay;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.vertex.ElevatorHopVertex;
import org.opentripplanner.street.model.vertex.OsmEntityType;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.streetadapter.VertexFactory;

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
    var elevatorNode = node(2, new WgsCoordinate(1, 0));
    elevatorNode.addTag("duration", "00:01:02");
    elevatorNode.addTag("highway", "elevator");
    elevatorNode.addTag("level", "1;2");
    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.addTag("level", "1"), node0, elevatorNode)
      .addWayFromNodes(way -> way.addTag("level", "2"), node1, elevatorNode)
      .build();
    var graph = new Graph();

    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    var edges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(edges).hasSize(2);
    for (var edge : edges) {
      assertThat(edge.getTravelTime()).hasValue(Duration.ofSeconds(62));
    }
  }

  @Test
  void testMultilevelNodeWithWaysOnSameLevel() {
    var n1 = node(1, new WgsCoordinate(0, 1));
    var n2 = node(2, new WgsCoordinate(0, 2));
    var elevatorNode = node(3, new WgsCoordinate(0, 3));
    elevatorNode.addTag("highway", "elevator");
    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.addTag("level", "1"), n1, elevatorNode)
      .addWayFromNodes(way -> way.addTag("level", "1"), n2, elevatorNode)
      .build();
    var graph = new Graph();

    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    var edges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(edges).hasSize(2);
    for (var edge : edges) {
      assertEquals(edge.getLevels(), 0.0);
    }
  }

  @Test
  void testMultilevelNodeWithMultipleWays() {
    var n1 = node(1, new WgsCoordinate(0, 1));
    var n2 = node(2, new WgsCoordinate(0, 2));
    var n3 = node(3, new WgsCoordinate(0, 3));
    var n4 = node(4, new WgsCoordinate(0, 4));
    var elevatorNode = node(5, new WgsCoordinate(0, 5));
    elevatorNode.addTag("highway", "elevator");

    var way1 = new OsmWay();
    way1.setId(1);
    way1.addTag("highway", "corridor");
    way1.addTag("level", "0");
    way1.addNodeRef(1);
    way1.addNodeRef(5);
    var way2 = new OsmWay();
    way2.setId(2);
    way2.addTag("highway", "corridor");
    way2.addTag("level", "2");
    way2.addNodeRef(2);
    way2.addNodeRef(5);
    var way3 = new OsmWay();
    way3.setId(3);
    way3.addTag("highway", "corridor");
    way3.addTag("level", "2");
    way3.addNodeRef(3);
    way3.addNodeRef(5);
    var way4 = new OsmWay();
    way4.setId(4);
    way4.addTag("highway", "corridor");
    way4.addTag("level", "3");
    way4.addNodeRef(4);
    way4.addNodeRef(5);

    var provider = new TestOsmProvider(
      List.of(),
      List.of(way1, way2, way3, way4),
      List.of(n1, n2, n3, n4, elevatorNode)
    );
    var graph = new Graph();
    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    VertexFactory vertexFactory = new VertexFactory(new Graph());

    var osmElevatorVertex1 = vertexFactory.osmElevator(
      elevatorNode,
      OsmEntityType.WAY,
      way1.getId()
    );
    var osmElevatorVertex2 = vertexFactory.osmElevator(
      elevatorNode,
      OsmEntityType.WAY,
      way2.getId()
    );
    var osmElevatorVertex3 = vertexFactory.osmElevator(
      elevatorNode,
      OsmEntityType.WAY,
      way3.getId()
    );
    var osmElevatorVertex4 = vertexFactory.osmElevator(
      elevatorNode,
      OsmEntityType.WAY,
      way4.getId()
    );

    var elevatorVertex1 = vertexFactory.elevator(
      osmElevatorVertex1,
      osmElevatorVertex1.getLabelString()
    );
    var elevatorVertex2 = vertexFactory.elevator(
      osmElevatorVertex2,
      osmElevatorVertex2.getLabelString()
    );
    var elevatorVertex3 = vertexFactory.elevator(
      osmElevatorVertex3,
      osmElevatorVertex3.getLabelString()
    );
    var elevatorVertex4 = vertexFactory.elevator(
      osmElevatorVertex4,
      osmElevatorVertex4.getLabelString()
    );

    Set<String> actualEdgeSet = getActualEdgeSet(graph);
    Set<String> expectedEdgeSet = new HashSet<>();
    HashMap<Edge, Double> elevatorHopEdgeLevels = new HashMap<>();

    addElevatorBoardAndAlightEdges(expectedEdgeSet, osmElevatorVertex1, elevatorVertex1);
    addElevatorBoardAndAlightEdges(expectedEdgeSet, osmElevatorVertex2, elevatorVertex2);
    addElevatorBoardAndAlightEdges(expectedEdgeSet, osmElevatorVertex3, elevatorVertex3);
    addElevatorBoardAndAlightEdges(expectedEdgeSet, osmElevatorVertex4, elevatorVertex4);

    addElevatorHopEdges(
      elevatorVertex1,
      elevatorVertex2,
      2,
      expectedEdgeSet,
      elevatorHopEdgeLevels
    );
    addElevatorHopEdges(
      elevatorVertex2,
      elevatorVertex3,
      0,
      expectedEdgeSet,
      elevatorHopEdgeLevels
    );
    addElevatorHopEdges(
      elevatorVertex3,
      elevatorVertex4,
      1,
      expectedEdgeSet,
      elevatorHopEdgeLevels
    );

    assertEquals(expectedEdgeSet, actualEdgeSet);
    int streetEdgeCount = 8;
    assertEquals(expectedEdgeSet.size() + streetEdgeCount, graph.getEdges().size());

    graph
      .getEdgesOfType(ElevatorHopEdge.class)
      .stream()
      .forEach(edge -> elevatorHopEdgeLevels.put(edge, edge.getLevels()));
    for (var edge : graph.getEdgesOfType(ElevatorHopEdge.class)) {
      assertEquals(edge.getLevels(), elevatorHopEdgeLevels.get(edge));
    }
  }

  @Test
  void testMultilevelWay() {
    var n1 = node(1, new WgsCoordinate(0, 1));
    var n2 = node(2, new WgsCoordinate(0, 2));

    var elevatorWay = new OsmWay();
    elevatorWay.setId(1);
    elevatorWay.addTag("highway", "elevator");
    elevatorWay.addTag("level", "1;3.5");
    elevatorWay.addNodeRef(1);
    elevatorWay.addNodeRef(2);

    var provider = new TestOsmProvider(List.of(), List.of(elevatorWay), List.of(n1, n2));
    var graph = new Graph();
    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    VertexFactory vertexFactory = new VertexFactory(new Graph());
    Set<String> edgeSet = new HashSet<>();

    var osmVertex1 = new OsmVertex(0, 1, 1);
    var osmVertex2 = new OsmVertex(0, 2, 2);

    var elevatorHopVertex1 = vertexFactory.elevator(
      osmVertex1,
      elevatorWay.getId() + "_" + 0 + "_" + osmVertex1.getLabelString()
    );
    var elevatorHopVertex2 = vertexFactory.elevator(
      osmVertex2,
      elevatorWay.getId() + "_" + 1 + "_" + osmVertex2.getLabelString()
    );

    addElevatorBoardAndAlightEdges(edgeSet, osmVertex1, elevatorHopVertex1);
    addElevatorBoardAndAlightEdges(edgeSet, osmVertex2, elevatorHopVertex2);
    addElevatorHopEdges(elevatorHopVertex1, elevatorHopVertex2, 2.5, edgeSet, null);

    assertEquals(
      edgeSet,
      new HashSet<>(
        graph
          .getEdges()
          .stream()
          .map(edge -> convertEdgeToVertexLabelString(edge))
          .toList()
      )
    );
    assertEquals(edgeSet.size(), graph.getEdges().size());
  }

  @Test
  void testMultilevelWayWithoutLevelInfo() {
    var n1 = node(1, new WgsCoordinate(0, 1));
    var n2 = node(2, new WgsCoordinate(0, 2));

    var elevatorWay = new OsmWay();
    elevatorWay.setId(1);
    elevatorWay.addTag("highway", "elevator");
    elevatorWay.addNodeRef(1);
    elevatorWay.addNodeRef(2);

    var provider = new TestOsmProvider(List.of(), List.of(elevatorWay), List.of(n1, n2));
    var graph = new Graph();
    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    VertexFactory vertexFactory = new VertexFactory(new Graph());
    Set<String> edgeSet = new HashSet<>();

    var osmVertex1 = new OsmVertex(0, 1, 1);
    var osmVertex2 = new OsmVertex(0, 2, 2);

    var elevatorHopVertex1 = vertexFactory.elevator(
      osmVertex1,
      elevatorWay.getId() + "_" + 0 + "_" + osmVertex1.getLabelString()
    );
    var elevatorHopVertex2 = vertexFactory.elevator(
      osmVertex2,
      elevatorWay.getId() + "_" + 1 + "_" + osmVertex2.getLabelString()
    );

    addElevatorBoardAndAlightEdges(edgeSet, osmVertex1, elevatorHopVertex1);
    addElevatorBoardAndAlightEdges(edgeSet, osmVertex2, elevatorHopVertex2);
    addElevatorHopEdges(elevatorHopVertex1, elevatorHopVertex2, 0, edgeSet, null);

    assertEquals(
      edgeSet,
      new HashSet<>(
        graph
          .getEdges()
          .stream()
          .map(edge -> convertEdgeToVertexLabelString(edge))
          .toList()
      )
    );
    assertEquals(edgeSet.size(), graph.getEdges().size());
  }

  @ParameterizedTest
  @CsvSource(
    value = {
      "1, 2, 3, 4", "1, 1, 1, 1", "0, 1, 1, null", "null, null, 1, null", "null, null, null, null",
    },
    nullValues = "null"
  )
  void testOsmElevatorNodeUniqueLabels(String level1, String ref1, String level2, String ref2) {
    var n1 = node(1, new WgsCoordinate(0, 1));
    var n2 = node(2, new WgsCoordinate(0, 2));
    var elevatorNode = node(3, new WgsCoordinate(0, 3));
    elevatorNode.addTag("highway", "elevator");
    var provider = TestOsmProvider.of()
      .addWayFromNodes(
        way -> {
          way.addTag("level", level1);
          way.addTag("level:ref", ref1);
        },
        n1,
        elevatorNode
      )
      .addWayFromNodes(
        way -> {
          way.addTag("level", level2);
          way.addTag("level:ref", ref2);
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
      .addWayFromNodes(way -> way.addTag("highway", "elevator"), n1, n2, n3)
      .addWayFromNodes(way -> way.addTag("public_transport", "platform"), n4, n2, n5)
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

  /**
   * If the connected nodes of an elevator way have been modeled as elevators, they do not appear
   * as intersection nodes. OTP should create an issue, but it should not cause errors.
   */
  @Test
  void elevatorWayWithFewerThanTwoIntersectionNodes() {
    var n1 = node(1, new WgsCoordinate(1, 1));
    n1.addTag("highway", "elevator");
    var n2 = node(2, new WgsCoordinate(2, 2));
    n2.addTag("highway", "elevator");

    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.addTag("highway", "elevator"), n1, n2)
      .build();
    var graph = new Graph();
    var issueStore = new DefaultDataImportIssueStore();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withIssueStore(issueStore)
      .build()
      .buildGraph();

    var elevatorHopEdges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(elevatorHopEdges).hasSize(0);

    var issues = issueStore
      .listIssues()
      .stream()
      .filter(issue -> issue instanceof FewerThanTwoIntersectionNodesInElevatorWay)
      .map(FewerThanTwoIntersectionNodesInElevatorWay.class::cast)
      .toList();
    assertEquals(1, issues.size());
    assertEquals(0, issues.getFirst().intersectionNodes());
  }

  private void addElevatorBoardAndAlightEdges(
    Set<String> edgeSet,
    OsmVertex osmVertex,
    ElevatorHopVertex elevatorVertex
  ) {
    edgeSet.add(
      convertEdgeToVertexLabelString(
        ElevatorBoardEdge.createElevatorBoardEdge(osmVertex, elevatorVertex)
      )
    );
    edgeSet.add(
      convertEdgeToVertexLabelString(
        ElevatorAlightEdge.createElevatorAlightEdge(elevatorVertex, osmVertex)
      )
    );
  }

  private void addElevatorHopEdges(
    ElevatorHopVertex elevatorVertex1,
    ElevatorHopVertex elevatorVertex2,
    double levels,
    Set<String> edgeSet,
    HashMap<Edge, Double> elevatorHopEdgeLevels
  ) {
    var edge1 = ElevatorHopEdge.createElevatorHopEdge(
      elevatorVertex1,
      elevatorVertex2,
      StreetTraversalPermission.PEDESTRIAN,
      Accessibility.NO_INFORMATION,
      levels,
      -1
    );
    var edge2 = ElevatorHopEdge.createElevatorHopEdge(
      elevatorVertex2,
      elevatorVertex1,
      StreetTraversalPermission.PEDESTRIAN,
      Accessibility.NO_INFORMATION,
      levels,
      -1
    );

    if (elevatorHopEdgeLevels != null) {
      elevatorHopEdgeLevels.put(edge1, edge1.getLevels());
      elevatorHopEdgeLevels.put(edge2, edge2.getLevels());
    }

    edgeSet.add(convertEdgeToVertexLabelString(edge1));
    edgeSet.add(convertEdgeToVertexLabelString(edge2));
  }

  private Set<String> getActualEdgeSet(Graph graph) {
    Set<String> actualEdgeSet = new HashSet<>();
    actualEdgeSet.addAll(
      graph
        .getEdgesOfType(ElevatorBoardEdge.class)
        .stream()
        .map(edge -> convertEdgeToVertexLabelString(edge))
        .toList()
    );
    actualEdgeSet.addAll(
      graph
        .getEdgesOfType(ElevatorAlightEdge.class)
        .stream()
        .map(edge -> convertEdgeToVertexLabelString(edge))
        .toList()
    );
    actualEdgeSet.addAll(
      graph
        .getEdgesOfType(ElevatorHopEdge.class)
        .stream()
        .map(edge -> convertEdgeToVertexLabelString(edge))
        .toList()
    );
    return actualEdgeSet;
  }

  private String convertEdgeToVertexLabelString(Edge edge) {
    return edge.getFromVertex().getLabelString() + "-" + edge.getToVertex().getLabelString();
  }
}
