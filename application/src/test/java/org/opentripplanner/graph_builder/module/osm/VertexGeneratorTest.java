package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.opentripplanner.graph_builder.module.osm.LinearBarrierNodeType.NORMAL;
import static org.opentripplanner.graph_builder.module.osm.LinearBarrierNodeType.SPLIT;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.issues.BarrierIntersectingHighway;
import org.opentripplanner.graph_builder.issues.DifferentLevelsSharingBarrier;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.vertex.BarrierPassThroughVertex;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;

class VertexGeneratorTest {

  @Test
  void testBarrierGenerator() {
    Graph graph = new Graph();
    OsmDatabase osmdb = new OsmDatabase(DataImportIssueStore.NOOP);

    var n1 = OsmNode.of().withId(1).withLatLon(0, 0).build();
    var n2 = OsmNode.of().withId(2).withLatLon(0, 1).build();
    var n3 = OsmNode.of().withId(3).withLatLon(0, 2).build();
    var n4 = OsmNode.of().withId(4).withLatLon(1, 2).build();
    var n5 = OsmNode.of().withId(5).withLatLon(1, 1).build();
    var n10 = OsmNode.of().withId(10).withLatLon(2, 2).build();

    var chain = OsmWay.of().withId(999).withTag("barrier", "chain").addNodeRef(1, 2, 3).build();

    var circularChain = OsmWay.of()
      .withId(998)
      .withTag("barrier", "chain")
      .addNodeRef(3, 4, 5, 3)
      .build();

    var w1 = OsmWay.of()
      .withId(1)
      .withTag("highway", "pedestrian")
      .withTag("level", "0")
      .addNodeRef(4, 10, 3, 4)
      .build();

    var w2 = OsmWay.of()
      .withId(2)
      .withTag("highway", "pedestrian")
      .withTag("level", "1")
      .withTag("area", "yes")
      .addNodeRef(30, 3, 10, 30)
      .build();

    osmdb.addWay(chain);
    osmdb.addWay(circularChain);
    osmdb.addWay(w1);
    osmdb.addWay(w2);
    osmdb.doneSecondPhaseWays();
    // only 3, 4 and 10 are kept because nodes not on routable ways are not kept
    osmdb.addNode(n1);
    osmdb.addNode(n2);
    osmdb.addNode(n3);
    osmdb.addNode(n4);
    osmdb.addNode(n5);
    osmdb.addNode(n10);

    var issueStore = new DefaultDataImportIssueStore();
    var subject = new VertexGenerator(osmdb, graph, Set.of(), false, issueStore);
    subject.initNodesInBarrierWays();

    var nodesInBarrierWays = subject.nodesInBarrierWays();
    // 1 kept node on chain and 2 kept nodes on circular chain
    assertEquals(3, nodesInBarrierWays.size());
    assertEquals(2, nodesInBarrierWays.get(n3).size());
    assertEquals(1, nodesInBarrierWays.get(n4).size());

    var vertexForW1OnBarrier = subject.getVertexForOsmNode(n3, w1, SPLIT);
    var vertexForW2OnBarrier = subject.getVertexForOsmNode(n3, w2, SPLIT);
    var vertexForW1NotOnBarrier = subject.getVertexForOsmNode(n10, w1, SPLIT);
    var vertexForW2NotOnBarrier = subject.getVertexForOsmNode(n10, w2, SPLIT);

    assertNotEquals(vertexForW1OnBarrier, vertexForW2OnBarrier);
    assertEquals(vertexForW1NotOnBarrier, vertexForW2NotOnBarrier);

    assertInstanceOf(BarrierPassThroughVertex.class, vertexForW2OnBarrier);
    assertEquals(n3.getId(), ((BarrierPassThroughVertex) vertexForW2OnBarrier).nodeId());
    assertEquals(w2.getId(), ((BarrierPassThroughVertex) vertexForW2OnBarrier).getEntityId());
    assertFalse(vertexForW2NotOnBarrier instanceof BarrierPassThroughVertex);

    Map<OsmNode, Map<OsmEntity, OsmVertex>> splitVerticesOnBarriers =
      subject.splitVerticesOnBarriers();
    assertEquals(1, splitVerticesOnBarriers.size());
    assertEquals(
      Map.of(w1, vertexForW1OnBarrier, w2, vertexForW2OnBarrier),
      splitVerticesOnBarriers.get(n3)
    );

    assertEquals(
      0,
      issueStore
        .listIssues()
        .stream()
        .filter(x -> x instanceof BarrierIntersectingHighway)
        .count()
    );
    var barrierVertexOnBarrier = subject.getVertexForOsmNode(n3, w1, NORMAL);
    assertInstanceOf(OsmVertex.class, barrierVertexOnBarrier);
    assertFalse(barrierVertexOnBarrier instanceof BarrierVertex);
    assertEquals(
      1,
      issueStore
        .listIssues()
        .stream()
        .filter(x -> x instanceof BarrierIntersectingHighway)
        .count()
    );
    var barrierVertexNotOnBarrier = subject.getVertexForOsmNode(n10, w1, NORMAL);
    assertFalse(barrierVertexNotOnBarrier instanceof BarrierVertex);

    subject.getVertexForOsmNode(n4, w1, SPLIT);
    subject.getVertexForOsmNode(n4, w2, SPLIT);

    subject.createDifferentLevelsSharingBarrierIssues();
    var issues = getBarrierLevelIssues(issueStore);
    assertEquals(2, issues.length);
    assertEquals(3, issues[0].node().getId());
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
