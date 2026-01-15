package org.opentripplanner.graph_builder.module.osm.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.issues.BarrierIntersectingHighway;
import org.opentripplanner.graph_builder.issues.DifferentLevelsSharingBarrier;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.vertex.BarrierPassThroughVertex;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.Vertex;

public class BarrierTest {

  @Test
  void testLinearCrossingNonIntersection() {
    var way = new OsmWay();
    way.addTag("highway", "path");
    way.addNodeRef(1);
    way.addNodeRef(2);
    way.addNodeRef(3);
    way.addNodeRef(4);
    way.setId(1);

    var barrier = new OsmWay();
    barrier.addTag("barrier", "fence");
    barrier.addNodeRef(99);
    barrier.addNodeRef(2);
    barrier.addNodeRef(98);
    barrier.setId(2);

    var osmProvider = new TestOsmProvider(
      List.of(),
      List.of(way, barrier),
      Set.of(1, 2, 3, 4, 98, 99)
        .stream()
        .map(id -> {
          var node = new OsmNode((double) id / 1000, 0);
          node.setId(id);
          return node;
        })
        .toList()
    );

    var graph = new Graph();
    var issueStore = new DefaultDataImportIssueStore();

    OsmModuleTestFactory.of(osmProvider)
      .withGraph(graph)
      .builder()
      .withIssueStore(issueStore)
      .build()
      .buildGraph();

    assertEquals(3, graph.getVertices().size());
    var barrierVertices = graph.getVerticesOfType(BarrierVertex.class);
    assertEquals(0, barrierVertices.size());
    var issues = issueStore
      .listIssues()
      .stream()
      .filter(issue -> issue instanceof BarrierIntersectingHighway);
    assertEquals(1, issues.count());
  }

  @Test
  void testHighwayReachingBarrierOnArea() {
    var n1 = new OsmNode(0, 0);
    n1.setId(1);
    var n2 = new OsmNode(0.001, 0);
    n2.setId(2);
    var n3 = new OsmNode(0, -0.001);
    n3.setId(3);
    var n4 = new OsmNode(0, 0.001);
    n4.setId(4);
    var n5 = new OsmNode(-0.001, 0.001);
    n5.setId(5);
    var n6 = new OsmNode(-0.001, -0.001);
    n6.setId(6);

    var path = new OsmWay();
    path.addTag("highway", "path");
    path.addNodeRef(1);
    path.addNodeRef(2);
    path.setId(1);

    var chain = new OsmWay();
    chain.addTag("barrier", "chain");
    chain.addNodeRef(3);
    chain.addNodeRef(1);
    chain.addNodeRef(4);
    chain.setId(2);

    var barrier = new OsmWay();
    barrier.addTag("highway", "pedestrian");
    barrier.addTag("bicycle", "yes");
    barrier.addTag("area", "yes");
    barrier.addNodeRef(1);
    barrier.addNodeRef(4);
    barrier.addNodeRef(5);
    barrier.addNodeRef(6);
    barrier.addNodeRef(3);
    barrier.addNodeRef(1);

    var osmProvider = new TestOsmProvider(
      List.of(),
      List.of(path, chain, barrier),
      List.of(n1, n2, n3, n4, n5, n6)
    );
    new OsmTagMapper().populateProperties(osmProvider.getWayPropertySet());

    var graph = new Graph();

    var subject = OsmModuleTestFactory.of(osmProvider).withGraph(graph).builder().build();

    subject.buildGraph();

    // the vertex for node 1 has been split, one for the area and one for the crossing of the linear
    // way
    Collection<Vertex> vertices = graph.getVertices();
    assertEquals(7, vertices.size());
    assertEquals(3, graph.getVerticesOfType(BarrierPassThroughVertex.class).size());
    assertEquals(
      2,
      graph.getVerticesOfType(OsmVertex.class).stream().filter(v -> v.nodeId() == 1).toList().size()
    );

    // check traversal permission starting from node 2
    var v2 = graph
      .getVerticesOfType(OsmVertex.class)
      .stream()
      .filter(v -> v.nodeId() == 2)
      .findFirst()
      .orElseThrow();
    assertEquals(1, v2.getOutgoing().size());
    // we first reach the barrier crossing on the path at v1
    var v1OnPath = v2.getOutgoingStreetEdges().getFirst().getToVertex();
    assertInstanceOf(OsmVertex.class, v1OnPath);
    // at that barrier crossing, we can either return to the origin or enter the area
    assertEquals(2, v1OnPath.getOutgoing().size());
    // we then enter the area
    var barrierCrossing = v1OnPath
      .getOutgoingStreetEdges()
      .stream()
      .filter(e -> e.getToVertex() instanceof BarrierPassThroughVertex)
      .findFirst()
      .orElseThrow();
    assertEquals(PEDESTRIAN, barrierCrossing.getPermission());
    var v1OnArea = barrierCrossing.getToVertex();
    for (var edge : v1OnArea.getOutgoingStreetEdges()) {
      // we check that we can cycle freely within the area, not encumbered by the chain
      if (edge instanceof AreaEdge) {
        assertEquals(PEDESTRIAN_AND_BICYCLE, edge.getPermission());
      }
    }
  }

  @Test
  void testDifferentLevelsConnectingBarrier() {
    var n1 = new OsmNode(0, 0);
    n1.setId(1);
    var n2 = new OsmNode(0, 1);
    n2.setId(2);
    var n3 = new OsmNode(0, 2);
    n3.setId(3);
    var n4 = new OsmNode(0, 3);
    n4.setId(4);
    n4.addTag("barrier", "bollard");
    var n5 = new OsmNode(1, 0);
    n5.setId(5);
    var n6 = new OsmNode(-1, 0);
    n6.setId(6);
    n6.addTag("barrier", "bollard");

    var chain = new OsmWay();
    chain.addTag("barrier", "chain");
    chain.setId(999);
    chain.addNodeRef(1);
    chain.addNodeRef(2);
    chain.addNodeRef(3);

    var w1 = new OsmWay();
    w1.setId(1);
    w1.addTag("highway", "pedestrian");
    w1.addTag("level", "0");
    w1.addTag("area", "yes");
    w1.addNodeRef(4);
    w1.addNodeRef(5);
    w1.addNodeRef(1);
    w1.addNodeRef(4);

    var w2 = new OsmWay();
    w2.setId(2);
    w2.addTag("highway", "pedestrian");
    w2.addTag("level", "1");
    w2.addTag("area", "yes");
    w2.addNodeRef(1);
    w2.addNodeRef(2);
    w2.addNodeRef(3);
    w2.addNodeRef(6);
    w2.addNodeRef(1);

    var w3 = new OsmWay();
    w3.setId(3);
    w3.addTag("highway", "pedestrian");
    w3.addTag("level", "1");
    w3.addTag("area", "yes");
    w3.addNodeRef(4);
    w3.addNodeRef(6);
    w3.addNodeRef(1);
    w3.addNodeRef(4);

    var issueStore = new DefaultDataImportIssueStore();
    var osmProvider = new TestOsmProvider(
      List.of(),
      List.of(w1, w2, w3, chain),
      List.of(n1, n2, n3, n4, n5, n6)
    );
    var osmDb = new OsmDatabase(issueStore);
    osmProvider.readOsm(osmDb);

    var osmModule = OsmModuleTestFactory.of(osmProvider)
      .builder()
      .withIssueStore(issueStore)
      .build();

    osmModule.buildGraph();

    var issues = getBarrierLevelIssues(issueStore);
    assertEquals(2, issues.length);
    assertEquals(1, issues[0].node().getId());
    assertEquals(4, issues[1].node().getId());
  }

  static DifferentLevelsSharingBarrier[] getBarrierLevelIssues(DataImportIssueStore issueStore) {
    return issueStore
      .listIssues()
      .stream()
      .filter(issue -> issue instanceof DifferentLevelsSharingBarrier)
      .map(x -> (DifferentLevelsSharingBarrier) x)
      .sorted(Comparator.comparingLong(x -> x.node().getId()))
      .toArray(DifferentLevelsSharingBarrier[]::new);
  }
}
