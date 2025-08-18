package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.opentripplanner.graph_builder.module.osm.LinearBarrierNodeType.BARRIER_VERTEX;
import static org.opentripplanner.graph_builder.module.osm.LinearBarrierNodeType.SPLIT;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.OsmVertexOnWay;

class VertexGeneratorTest {

  @Test
  void testBarrierGenerator() {
    Graph graph = new Graph();
    OsmDatabase osmdb = new OsmDatabase(DataImportIssueStore.NOOP);

    var n1 = new OsmNode(0, 0);
    n1.setId(1);
    var n2 = new OsmNode(0, 1);
    n2.setId(2);
    var n3 = new OsmNode(0, 2);
    n3.setId(3);
    var n4 = new OsmNode(1, 2);
    n4.setId(4);
    var n5 = new OsmNode(1, 1);
    n5.setId(5);
    var n10 = new OsmNode(2, 2);
    n10.setId(10);

    var chain = new OsmWay();
    chain.addTag("barrier", "chain");
    chain.setId(999);
    chain.addNodeRef(1);
    chain.addNodeRef(2);
    chain.addNodeRef(3);

    var circularChain = new OsmWay();
    circularChain.addTag("barrier", "chain");
    circularChain.setId(998);
    circularChain.addNodeRef(3);
    circularChain.addNodeRef(4);
    circularChain.addNodeRef(5);
    circularChain.addNodeRef(3);

    var w1 = new OsmWay();
    w1.setId(1);
    w1.addTag("highway", "path");
    w1.addNodeRef(4);
    w1.addNodeRef(10);

    var w2 = new OsmWay();
    w2.setId(2);
    w2.addTag("highway", "path");
    w2.addNodeRef(30);
    w2.addNodeRef(3);
    w2.addNodeRef(10);

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

    var subject = new VertexGenerator(osmdb, graph, Set.of(), false);
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

    assertInstanceOf(OsmVertexOnWay.class, vertexForW2OnBarrier);
    assertEquals(n3.getId(), ((OsmVertexOnWay) vertexForW2OnBarrier).nodeId);
    assertEquals(w2.getId(), ((OsmVertexOnWay) vertexForW2OnBarrier).wayId);
    assertFalse(vertexForW2NotOnBarrier instanceof OsmVertexOnWay);

    Map<OsmNode, Map<OsmEntity, OsmVertex>> splitVerticesOnBarriers =
      subject.splitVerticesOnBarriers();
    assertEquals(1, splitVerticesOnBarriers.size());
    assertEquals(
      Map.of(w1, vertexForW1OnBarrier, w2, vertexForW2OnBarrier),
      splitVerticesOnBarriers.get(n3)
    );

    var barrierVertexOnBarrier = subject.getVertexForOsmNode(n3, w1, BARRIER_VERTEX);
    assertInstanceOf(BarrierVertex.class, barrierVertexOnBarrier);
    assertEquals(PEDESTRIAN, ((BarrierVertex) barrierVertexOnBarrier).getBarrierPermissions());
    var barrierVertexNotOnBarrier = subject.getVertexForOsmNode(n10, w1, BARRIER_VERTEX);
    assertFalse(barrierVertexNotOnBarrier instanceof BarrierVertex);
  }
}
