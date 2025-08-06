package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.routing.graph.Graph;
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

    var w2 = new OsmWay();
    w2.setId(2);
    w2.addTag("highway", "path");

    osmdb.addWay(chain);
    osmdb.addWay(circularChain);
    osmdb.doneSecondPhaseWays();
    osmdb.addNode(n1);
    osmdb.addNode(n2);
    osmdb.addNode(n3);
    osmdb.addNode(n4);
    osmdb.addNode(n5);

    var subject = new VertexGenerator(osmdb, graph, Set.of(), false);
    subject.initNodesInBarrierWays();

    var nodesInBarrierWays = subject.nodesInBarrierWays();
    // 3 nodes on chain and 3 nodes on circular chain
    assertEquals(6, nodesInBarrierWays.size());
    assertEquals(1, nodesInBarrierWays.get(n1).size());
    assertEquals(1, nodesInBarrierWays.get(n2).size());
    assertEquals(2, nodesInBarrierWays.get(n3).size());
    assertEquals(1, nodesInBarrierWays.get(n4).size());
    assertEquals(1, nodesInBarrierWays.get(n5).size());

    var vertexForW1OnBarrier = subject.getVertexForOsmNode(n2, w1);
    var vertexForW2OnBarrier = subject.getVertexForOsmNode(n2, w2);
    var vertexForW1AtEndOfBarrier = subject.getVertexForOsmNode(n1, w1);
    var vertexForW2AtEndOfBarrier = subject.getVertexForOsmNode(n1, w2);

    assertNotEquals(vertexForW1OnBarrier, vertexForW2OnBarrier);
    assertNotEquals(vertexForW1AtEndOfBarrier, vertexForW2AtEndOfBarrier);

    assertInstanceOf(OsmVertexOnWay.class, vertexForW2OnBarrier);
    assertEquals(n2.getId(), ((OsmVertexOnWay) vertexForW2OnBarrier).nodeId);
    assertEquals(w2.getId(), ((OsmVertexOnWay) vertexForW2OnBarrier).wayId);
    assertInstanceOf(OsmVertexOnWay.class, vertexForW2AtEndOfBarrier);

    Map<OsmNode, Map<OsmEntity, OsmVertex>> splitVerticesOnBarriers =
      subject.splitVerticesOnBarriers();
    assertEquals(2, splitVerticesOnBarriers.size());
    assertEquals(
      Map.of(w1, vertexForW1OnBarrier, w2, vertexForW2OnBarrier),
      splitVerticesOnBarriers.get(n2)
    );
  }
}
