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
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.vertex.BarrierPassThroughVertex;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.Vertex;

public class BarrierTest {

  @Test
  void testLinearCrossingNonIntersection() {
    var way = OsmWay.of().withId(1).withTag("highway", "path").addNodeRef(1, 2, 3, 4).build();

    var barrier = OsmWay.of().withId(2).withTag("barrier", "fence").addNodeRef(99, 2, 98).build();

    var osmProvider = new TestOsmProvider(
      List.of(),
      List.of(way, barrier),
      Set.of(1, 2, 3, 4, 98, 99)
        .stream()
        .map(id -> OsmNode.of().withId(id).withLatLon((double) id / 1000, 0).build())
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
    var n1 = OsmNode.of().withId(1).withLatLon(0, 0).build();
    var n2 = OsmNode.of().withId(2).withLatLon(0.001, 0).build();
    var n3 = OsmNode.of().withId(3).withLatLon(0, -0.001).build();
    var n4 = OsmNode.of().withId(4).withLatLon(0, 0.001).build();
    var n5 = OsmNode.of().withId(5).withLatLon(-0.001, 0.001).build();
    var n6 = OsmNode.of().withId(6).withLatLon(-0.001, -0.001).build();

    var path = OsmWay.of().withId(1).withTag("highway", "path").addNodeRef(1, 2).build();

    var chain = OsmWay.of().withId(2).withTag("barrier", "chain").addNodeRef(3, 1, 4).build();

    var barrier = OsmWay.of()
      .withTag("highway", "pedestrian")
      .withTag("bicycle", "yes")
      .withTag("area", "yes")
      .addNodeRef(1, 4, 5, 6, 3, 1)
      .build();

    var osmProvider = new TestOsmProvider(
      List.of(),
      List.of(path, chain, barrier),
      List.of(n1, n2, n3, n4, n5, n6)
    );

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
      graph
        .getVerticesOfType(OsmVertex.class)
        .stream()
        .filter(v -> v.nodeId() == 1)
        .toList()
        .size()
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
    var n1 = OsmNode.of().withId(1).withLatLon(0, 0).build();
    var n2 = OsmNode.of().withId(2).withLatLon(0, 1).build();
    var n3 = OsmNode.of().withId(3).withLatLon(0, 2).build();
    var n4 = OsmNode.of().withId(4).withLatLon(0, 3).withTag("barrier", "bollard").build();
    var n5 = OsmNode.of().withId(5).withLatLon(1, 0).build();
    var n6 = OsmNode.of().withId(6).withLatLon(-1, 0).withTag("barrier", "bollard").build();

    var chain = OsmWay.of().withId(999).withTag("barrier", "chain").addNodeRef(1, 2, 3).build();

    var w1 = OsmWay.of()
      .withId(1)
      .withTag("highway", "pedestrian")
      .withTag("level", "0")
      .withTag("area", "yes")
      .addNodeRef(4, 5, 1, 4)
      .build();

    var w2 = OsmWay.of()
      .withId(2)
      .withTag("highway", "pedestrian")
      .withTag("level", "1")
      .withTag("area", "yes")
      .addNodeRef(1, 2, 3, 6, 1)
      .build();

    var w3 = OsmWay.of()
      .withId(3)
      .withTag("highway", "pedestrian")
      .withTag("level", "1")
      .withTag("area", "yes")
      .addNodeRef(4, 6, 1, 4)
      .build();

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
