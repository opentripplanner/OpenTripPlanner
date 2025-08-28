package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;
import static org.opentripplanner.street.search.TraverseMode.BICYCLE;
import static org.opentripplanner.street.search.TraverseMode.WALK;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.module.osm.naming.DefaultNamer;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.BarrierPassThroughVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;

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
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    connectToOutsideWorld(v1);
    subject.build(new OsmNode(), List.of(v1), List.of());
    assertEquals(1, v1.getDegreeIn());
    assertEquals(1, v1.getDegreeOut());
  }

  @Test
  void connectOneVertexWithBarrier() {
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    connectToOutsideWorld(v1);
    subject.build(new OsmNode(), List.of(v1), List.of(WALL));
    assertEquals(1, v1.getDegreeIn());
    assertEquals(1, v1.getDegreeOut());
  }

  @Test
  void connectThreeVerticesWithoutBarrier() {
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    var v2 = new BarrierPassThroughVertex(0, 0, 0, 2);
    var v3 = new BarrierPassThroughVertex(0, 0, 0, 3);
    connectToOutsideWorld(v1, v2, v3);
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of());
    assertEquals(3, v1.getDegreeIn());
    assertEquals(3, v1.getDegreeOut());
    assertEquals(3, v2.getDegreeIn());
    assertEquals(3, v2.getDegreeOut());
    assertEquals(3, v3.getDegreeIn());
    assertEquals(3, v3.getDegreeOut());
    for (var edge : getEdgesThroughBarrierFromVertex(v1)) {
      assertEquals(ALL, edge.getPermission());
      assertTrue(edge.isWheelchairAccessible());
    }
  }

  @Test
  void connectThreeVerticesWithWall() {
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    var v2 = new BarrierPassThroughVertex(0, 0, 0, 2);
    var v3 = new BarrierPassThroughVertex(0, 0, 0, 3);
    connectToOutsideWorld(v1, v2, v3);

    // a wall can't be passed with any means so no edges should be created
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of(WALL));
    assertEquals(1, v1.getDegreeIn());
    assertEquals(1, v1.getDegreeOut());
    assertEquals(1, v2.getDegreeIn());
    assertEquals(1, v2.getDegreeOut());
    assertEquals(1, v3.getDegreeIn());
    assertEquals(1, v3.getDegreeOut());
  }

  @Test
  void connectThreeVerticesWithChain() {
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    var v2 = new BarrierPassThroughVertex(0, 0, 0, 2);
    var v3 = new BarrierPassThroughVertex(0, 0, 0, 3);
    connectToOutsideWorld(v1, v2, v3);

    // a chain is passable by pedestrians so edges should be created
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of(CHAIN));
    assertEquals(3, v1.getDegreeIn());
    assertEquals(3, v1.getDegreeOut());
    assertEquals(3, v2.getDegreeIn());
    assertEquals(3, v2.getDegreeOut());
    assertEquals(3, v3.getDegreeIn());
    assertEquals(3, v3.getDegreeOut());

    for (var edge : getEdgesThroughBarrierFromVertex(v1)) {
      assertEquals(PEDESTRIAN, edge.getPermission());
      assertFalse(edge.isWheelchairAccessible());
    }
  }

  private static List<StreetEdge> getEdgesThroughBarrierFromVertex(BarrierPassThroughVertex v1) {
    return v1
      .getOutgoingStreetEdges()
      .stream()
      .filter(e -> e.getToVertex() instanceof BarrierPassThroughVertex)
      .toList();
  }

  @Test
  void connectThreeVerticesWithKerb() {
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    var v2 = new BarrierPassThroughVertex(0, 0, 0, 2);
    var v3 = new BarrierPassThroughVertex(0, 0, 0, 3);
    connectToOutsideWorld(v1, v2, v3);

    // a kerb allows everything to get across but may pose a problem for wheelchair
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of(KERB));
    assertEquals(3, v1.getDegreeIn());
    assertEquals(3, v1.getDegreeOut());
    assertEquals(3, v2.getDegreeIn());
    assertEquals(3, v2.getDegreeOut());
    assertEquals(3, v3.getDegreeIn());
    assertEquals(3, v3.getDegreeOut());

    for (var edge : getEdgesThroughBarrierFromVertex(v1)) {
      assertEquals(ALL, edge.getPermission());
      assertFalse(edge.isWheelchairAccessible());
    }
  }

  @Test
  void connectThreeVerticesWithKerbAndHandrail() {
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    var v2 = new BarrierPassThroughVertex(0, 0, 0, 2);
    var v3 = new BarrierPassThroughVertex(0, 0, 0, 3);
    connectToOutsideWorld(v1, v2, v3);

    // a handrail intersects with a kerb, therefore only pedestrians can pass this barrier
    // intersection and wheelchair can't get through it
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of(KERB, HANDRAIL));
    assertEquals(3, v1.getDegreeIn());
    assertEquals(3, v1.getDegreeOut());
    assertEquals(3, v2.getDegreeIn());
    assertEquals(3, v2.getDegreeOut());
    assertEquals(3, v3.getDegreeIn());
    assertEquals(3, v3.getDegreeOut());

    for (var edge : getEdgesThroughBarrierFromVertex(v1)) {
      assertEquals(PEDESTRIAN, edge.getPermission());
      assertFalse(edge.isWheelchairAccessible());
    }
  }

  @Test
  void connectThreeVerticesWithKerbAndFence() {
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    var v2 = new BarrierPassThroughVertex(0, 0, 0, 2);
    var v3 = new BarrierPassThroughVertex(0, 0, 0, 3);
    connectToOutsideWorld(v1, v2, v3);

    // a fence intersects with a kerb, nothing can get through it
    subject.build(new OsmNode(), List.of(v1, v2, v3), List.of(KERB, FENCE));
    assertEquals(1, v1.getDegreeIn());
    assertEquals(1, v1.getDegreeOut());
    assertEquals(1, v2.getDegreeIn());
    assertEquals(1, v2.getDegreeOut());
    assertEquals(1, v3.getDegreeIn());
    assertEquals(1, v3.getDegreeOut());
  }

  @Test
  void connectThreeVerticesWithWallAndGate() {
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    var v2 = new BarrierPassThroughVertex(0, 0, 0, 2);
    var v3 = new BarrierPassThroughVertex(0, 0, 0, 3);
    connectToOutsideWorld(v1, v2, v3);

    var node = new OsmNode();
    node.addTag("barrier", "gate");
    node.addTag("access", "no");
    node.addTag("foot", "yes");

    // A gate can be used to pass the wall, so edges should be built
    subject.build(node, List.of(v1, v2, v3), List.of(WALL));
    assertEquals(3, v1.getDegreeIn());
    assertEquals(3, v1.getDegreeOut());
    assertEquals(3, v2.getDegreeIn());
    assertEquals(3, v2.getDegreeOut());
    assertEquals(3, v3.getDegreeIn());
    assertEquals(3, v3.getDegreeOut());
    for (var edge : getEdgesThroughBarrierFromVertex(v1)) {
      assertEquals(PEDESTRIAN, edge.getPermission());
      assertTrue(edge.isWheelchairAccessible());
    }
  }

  @Test
  void connectThreeVerticesWithWallAndBollard() {
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    var v2 = new BarrierPassThroughVertex(0, 0, 0, 2);
    var v3 = new BarrierPassThroughVertex(0, 0, 0, 3);
    connectToOutsideWorld(v1, v2, v3);

    var node = new OsmNode();
    node.addTag("barrier", "bollard");

    // I consider this tagging a hole on the wall, so edges should be built
    subject.build(node, List.of(v1, v2, v3), List.of(WALL));
    assertEquals(3, v1.getDegreeIn());
    assertEquals(3, v1.getDegreeOut());
    assertEquals(3, v2.getDegreeIn());
    assertEquals(3, v2.getDegreeOut());
    assertEquals(3, v3.getDegreeIn());
    assertEquals(3, v3.getDegreeOut());
    for (var edge : getEdgesThroughBarrierFromVertex(v1)) {
      assertEquals(PEDESTRIAN_AND_BICYCLE, edge.getPermission());
      assertTrue(edge.isWheelchairAccessible());
    }
  }

  @Test
  void throughOneWayTraffic() {
    var v0 = new OsmVertex(0, 0, 1);
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    var v2 = new BarrierPassThroughVertex(0, 0, 0, 2);
    var v3 = new OsmVertex(0, 0, 2);
    connect(v0, v1);
    connect(v2, v3);

    var node = new OsmNode();
    node.addTag("barrier", "bollard");

    subject.build(node, List.of(v1, v2), List.of(WALL));
    assertEquals(1, v1.getDegreeOut());
    assertEquals(1, v2.getDegreeOut());
    for (var edge : getEdgesThroughBarrierFromVertex(v1)) {
      assertEquals(PEDESTRIAN_AND_BICYCLE, edge.getPermission());
      assertTrue(edge.isWheelchairAccessible());
      assertFalse(edge.isWalkNoThruTraffic());
      assertFalse(edge.isBicycleNoThruTraffic());
    }
  }

  @Test
  void noThroughOneWayTraffic() {
    var v0 = new OsmVertex(0, 0, 1);
    var v1 = new BarrierPassThroughVertex(0, 0, 0, 1);
    var v2 = new BarrierPassThroughVertex(0, 0, 0, 2);
    var v3 = new OsmVertex(0, 0, 2);
    connect(v0, v1, WALK);
    connect(v2, v3, BICYCLE);

    var node = new OsmNode();
    node.addTag("barrier", "gate");

    subject.build(node, List.of(v1, v2), List.of(WALL));
    assertEquals(1, v1.getDegreeOut());
    assertEquals(1, v2.getDegreeOut());
    for (var edge : getEdgesThroughBarrierFromVertex(v1)) {
      assertEquals(ALL, edge.getPermission());
      assertTrue(edge.isWalkNoThruTraffic());
      assertTrue(edge.isBicycleNoThruTraffic());
      assertFalse(edge.isMotorVehicleNoThruTraffic());
    }
  }

  private static void connect(
    StreetVertex v1,
    StreetVertex v2,
    TraverseMode... noThruTraverseModes
  ) {
    var seb = new StreetEdgeBuilder<>()
      .withFromVertex(v1)
      .withToVertex(v2)
      .withPermission(ALL)
      .withGeometry(GeometryUtils.makeLineString(0, 0, 0, 1));
    for (var mode : noThruTraverseModes) {
      seb.withNoThruTrafficTraverseMode(mode);
    }
    seb.buildAndConnect();
  }

  /**
   * Make a bidirectional connection to a node elsewhere for each of the vertices given
   */
  private static void connectToOutsideWorld(StreetVertex... vertices) {
    var v0 = new OsmVertex(0, 0, 1);
    for (var v : vertices) {
      connect(v0, v);
      connect(v, v0);
    }
  }
}
