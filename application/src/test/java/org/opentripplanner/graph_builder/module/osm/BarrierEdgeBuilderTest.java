package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.naming.DefaultNamer;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.vertex.OsmVertexOnWay;

class BarrierEdgeBuilderTest {

  private static final EdgeNamer EDGE_NAMER = new DefaultNamer();
  private static final BarrierEdgeBuilder subject = new BarrierEdgeBuilder(EDGE_NAMER);

  private static final OsmWay WALL = (OsmWay) new OsmWay().addTag("barrier", "wall");
  private static final OsmWay FENCE = (OsmWay) new OsmWay().addTag("barrier", "fence");
  private static final OsmWay CHAIN = (OsmWay) new OsmWay().addTag("barrier", "chain");

  private static final OsmWay KERB = (OsmWay) new OsmWay().addTag("barrier", "kerb");

  private static final OsmWay HANDRAIL = (OsmWay) new OsmWay().addTag("barrier", "handrail");

  @Test
  void connectOneVertexWithoutBarrier() {
    var v1 = new OsmVertexOnWay(0, 0, 0, 1);
    subject.build(new OsmNode(), List.of(v1), List.of());
    assertEquals(0, v1.getDegreeIn());
    assertEquals(0, v1.getDegreeOut());
  }

  @Test
  void connectOneVertexWithBarrier() {
    var v1 = new OsmVertexOnWay(0, 0, 0, 1);
    subject.build(new OsmNode(), List.of(v1), List.of(WALL));
    assertEquals(0, v1.getDegreeIn());
    assertEquals(0, v1.getDegreeOut());
  }

  @Test
  void connectThreeVerticesWithoutBarrier() {
    var v1 = new OsmVertexOnWay(0, 0, 0, 1);
    var v2 = new OsmVertexOnWay(0, 0, 0, 2);
    var v3 = new OsmVertexOnWay(0, 0, 0, 3);
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of());
    assertEquals(2, v1.getDegreeIn());
    assertEquals(2, v1.getDegreeOut());
    assertEquals(2, v2.getDegreeIn());
    assertEquals(2, v2.getDegreeOut());
    assertEquals(2, v3.getDegreeIn());
    assertEquals(2, v3.getDegreeOut());
    for (var edge : v1.getOutgoingStreetEdges()) {
      assertEquals(ALL, edge.getPermission());
      assertTrue(edge.isWheelchairAccessible());
    }
  }

  @Test
  void connectThreeVerticesWithWall() {
    var v1 = new OsmVertexOnWay(0, 0, 0, 1);
    var v2 = new OsmVertexOnWay(0, 0, 0, 2);
    var v3 = new OsmVertexOnWay(0, 0, 0, 3);

    // a wall can't be passed with any means so no edges should be created
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of(WALL));
    assertEquals(0, v1.getDegreeIn());
    assertEquals(0, v1.getDegreeOut());
    assertEquals(0, v2.getDegreeIn());
    assertEquals(0, v2.getDegreeOut());
    assertEquals(0, v3.getDegreeIn());
    assertEquals(0, v3.getDegreeOut());
  }

  @Test
  void connectThreeVerticesWithChain() {
    var v1 = new OsmVertexOnWay(0, 0, 0, 1);
    var v2 = new OsmVertexOnWay(0, 0, 0, 2);
    var v3 = new OsmVertexOnWay(0, 0, 0, 3);

    // a chain is passable by pedestrians so edges should be created
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of(CHAIN));
    assertEquals(2, v1.getDegreeIn());
    assertEquals(2, v1.getDegreeOut());
    assertEquals(2, v2.getDegreeIn());
    assertEquals(2, v2.getDegreeOut());
    assertEquals(2, v3.getDegreeIn());
    assertEquals(2, v3.getDegreeOut());

    for (var edge : v1.getOutgoingStreetEdges()) {
      assertEquals(PEDESTRIAN, edge.getPermission());
      assertFalse(edge.isWheelchairAccessible());
    }
  }

  @Test
  void connectThreeVerticesWithKerb() {
    var v1 = new OsmVertexOnWay(0, 0, 0, 1);
    var v2 = new OsmVertexOnWay(0, 0, 0, 2);
    var v3 = new OsmVertexOnWay(0, 0, 0, 3);

    // a kerb allows everything to get across but may pose a problem for wheelchair
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of(KERB));
    assertEquals(2, v1.getDegreeIn());
    assertEquals(2, v1.getDegreeOut());
    assertEquals(2, v2.getDegreeIn());
    assertEquals(2, v2.getDegreeOut());
    assertEquals(2, v3.getDegreeIn());
    assertEquals(2, v3.getDegreeOut());

    for (var edge : v1.getOutgoingStreetEdges()) {
      assertEquals(ALL, edge.getPermission());
      assertFalse(edge.isWheelchairAccessible());
    }
  }

  @Test
  void connectThreeVerticesWithKerbAndHandrail() {
    var v1 = new OsmVertexOnWay(0, 0, 0, 1);
    var v2 = new OsmVertexOnWay(0, 0, 0, 2);
    var v3 = new OsmVertexOnWay(0, 0, 0, 3);

    // a handrail intersects with a kerb, therefore only pedestrians can pass this barrier
    // intersection and wheelchair can't get through it
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of(KERB, HANDRAIL));
    assertEquals(2, v1.getDegreeIn());
    assertEquals(2, v1.getDegreeOut());
    assertEquals(2, v2.getDegreeIn());
    assertEquals(2, v2.getDegreeOut());
    assertEquals(2, v3.getDegreeIn());
    assertEquals(2, v3.getDegreeOut());

    for (var edge : v1.getOutgoingStreetEdges()) {
      assertEquals(PEDESTRIAN, edge.getPermission());
      assertFalse(edge.isWheelchairAccessible());
    }
  }

  @Test
  void connectThreeVerticesWithKerbAndFence() {
    var v1 = new OsmVertexOnWay(0, 0, 0, 1);
    var v2 = new OsmVertexOnWay(0, 0, 0, 2);
    var v3 = new OsmVertexOnWay(0, 0, 0, 3);

    // a fence intersects with a kerb, nothing can get through it
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of(KERB, FENCE));
    assertEquals(0, v1.getDegreeIn());
    assertEquals(0, v1.getDegreeOut());
    assertEquals(0, v2.getDegreeIn());
    assertEquals(0, v2.getDegreeOut());
    assertEquals(0, v3.getDegreeIn());
    assertEquals(0, v3.getDegreeOut());
  }
}
