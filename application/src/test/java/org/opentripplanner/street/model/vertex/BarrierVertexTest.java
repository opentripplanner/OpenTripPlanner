package org.opentripplanner.street.model.vertex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;

/**
 * Created by mabu on 17.8.2015.
 */
public class BarrierVertexTest {

  @Test
  public void testBarrierPermissions() {
    OsmNode simpleBarrier = new OsmNode();
    assertFalse(simpleBarrier.isMotorVehicleBarrier());
    simpleBarrier.addTag("barrier", "bollard");
    assertTrue(simpleBarrier.isMotorVehicleBarrier());
    String label = "simpleBarrier";
    BarrierVertex bv = new BarrierVertex(simpleBarrier.lon, simpleBarrier.lat, 0);
    bv.setBarrierPermissions(
      simpleBarrier.overridePermissions(BarrierVertex.defaultBarrierPermissions, null)
    );
    assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, bv.getBarrierPermissions());

    simpleBarrier.addTag("foot", "yes");
    bv.setBarrierPermissions(
      simpleBarrier.overridePermissions(BarrierVertex.defaultBarrierPermissions, null)
    );
    assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, bv.getBarrierPermissions());
    simpleBarrier.addTag("bicycle", "yes");
    bv.setBarrierPermissions(
      simpleBarrier.overridePermissions(BarrierVertex.defaultBarrierPermissions, null)
    );
    assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, bv.getBarrierPermissions());
    simpleBarrier.addTag("access", "no");
    bv.setBarrierPermissions(
      simpleBarrier.overridePermissions(BarrierVertex.defaultBarrierPermissions, null)
    );
    assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, bv.getBarrierPermissions());

    simpleBarrier.addTag("motor_vehicle", "no");
    bv.setBarrierPermissions(
      simpleBarrier.overridePermissions(BarrierVertex.defaultBarrierPermissions, null)
    );
    assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, bv.getBarrierPermissions());

    simpleBarrier.addTag("bicycle", "no");
    bv.setBarrierPermissions(
      simpleBarrier.overridePermissions(BarrierVertex.defaultBarrierPermissions, null)
    );
    assertEquals(StreetTraversalPermission.PEDESTRIAN, bv.getBarrierPermissions());

    OsmNode complexBarrier = new OsmNode();
    complexBarrier.addTag("barrier", "bollard");
    complexBarrier.addTag("access", "no");

    bv.setBarrierPermissions(
      complexBarrier.overridePermissions(BarrierVertex.defaultBarrierPermissions, null)
    );
    assertEquals(StreetTraversalPermission.NONE, bv.getBarrierPermissions());

    OsmNode noBikeBollard = new OsmNode();
    noBikeBollard.addTag("barrier", "bollard");
    noBikeBollard.addTag("bicycle", "no");

    bv.setBarrierPermissions(
      noBikeBollard.overridePermissions(BarrierVertex.defaultBarrierPermissions, null)
    );
    assertEquals(StreetTraversalPermission.PEDESTRIAN, bv.getBarrierPermissions());

    /* test that traversal limitations work also without barrier tag  */
    OsmNode accessBarrier = new OsmNode();
    accessBarrier.addTag("access", "no");

    bv.setBarrierPermissions(
      accessBarrier.overridePermissions(BarrierVertex.defaultBarrierPermissions, null)
    );
    assertEquals(StreetTraversalPermission.NONE, bv.getBarrierPermissions());
  }

  @Test
  public void testStreetsWithBollard() {
    Graph graph = new Graph();
    BarrierVertex bv = new BarrierVertex(2.0, 2.0, 0);
    bv.setBarrierPermissions(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

    StreetVertex endVertex = StreetModelForTest.intersectionVertex("end_vertex", 1.0, 2.0);

    StreetEdge bv_to_endVertex_forward = edge(bv, endVertex, 100, false);

    assertTrue(bv_to_endVertex_forward.canTraverse(new TraverseModeSet(TraverseMode.CAR)));
    assertTrue(bv_to_endVertex_forward.canTraverse(new TraverseModeSet(TraverseMode.BICYCLE)));
    assertTrue(bv_to_endVertex_forward.canTraverse(new TraverseModeSet(TraverseMode.WALK)));

    assertFalse(bv_to_endVertex_forward.canTraverse(TraverseMode.CAR));
    assertTrue(bv_to_endVertex_forward.canTraverse(TraverseMode.BICYCLE));
    assertTrue(bv_to_endVertex_forward.canTraverse(TraverseMode.WALK));

    StreetEdge endVertex_to_bv_backward = edge(endVertex, bv, 100, true);

    assertTrue(endVertex_to_bv_backward.canTraverse(new TraverseModeSet(TraverseMode.CAR)));
    assertTrue(endVertex_to_bv_backward.canTraverse(new TraverseModeSet(TraverseMode.BICYCLE)));
    assertTrue(endVertex_to_bv_backward.canTraverse(new TraverseModeSet(TraverseMode.WALK)));

    assertFalse(endVertex_to_bv_backward.canTraverse(TraverseMode.CAR));
    assertTrue(endVertex_to_bv_backward.canTraverse(TraverseMode.BICYCLE));
    assertTrue(endVertex_to_bv_backward.canTraverse(TraverseMode.WALK));

    StreetEdge bv_to_endVertex_backward = edge(bv, endVertex, 100, true);

    assertTrue(bv_to_endVertex_backward.canTraverse(new TraverseModeSet(TraverseMode.CAR)));
    assertTrue(bv_to_endVertex_backward.canTraverse(new TraverseModeSet(TraverseMode.BICYCLE)));
    assertTrue(bv_to_endVertex_backward.canTraverse(new TraverseModeSet(TraverseMode.WALK)));

    assertFalse(bv_to_endVertex_backward.canTraverse(TraverseMode.CAR));
    assertTrue(bv_to_endVertex_backward.canTraverse(TraverseMode.BICYCLE));
    assertTrue(bv_to_endVertex_backward.canTraverse(TraverseMode.WALK));

    StreetEdge endVertex_to_bv_forward = edge(endVertex, bv, 100, false);

    assertTrue(endVertex_to_bv_forward.canTraverse(new TraverseModeSet(TraverseMode.CAR)));
    assertTrue(endVertex_to_bv_forward.canTraverse(new TraverseModeSet(TraverseMode.BICYCLE)));
    assertTrue(endVertex_to_bv_forward.canTraverse(new TraverseModeSet(TraverseMode.WALK)));

    assertFalse(endVertex_to_bv_forward.canTraverse(TraverseMode.CAR));
    assertTrue(endVertex_to_bv_forward.canTraverse(TraverseMode.BICYCLE));
    assertTrue(endVertex_to_bv_forward.canTraverse(TraverseMode.WALK));

    //tests bollard which allows only walking
    BarrierVertex onlyWalkBollard = new BarrierVertex(1.5, 1, 0);
    onlyWalkBollard.setBarrierPermissions(StreetTraversalPermission.PEDESTRIAN);
    StreetEdge edge = edge(onlyWalkBollard, endVertex, 100, false);

    assertTrue(edge.canTraverse(new TraverseModeSet(TraverseMode.CAR)));
    assertTrue(edge.canTraverse(new TraverseModeSet(TraverseMode.BICYCLE)));
    assertTrue(edge.canTraverse(new TraverseModeSet(TraverseMode.WALK)));

    assertFalse(edge.canTraverse(TraverseMode.CAR));
    assertFalse(edge.canTraverse(TraverseMode.BICYCLE));
    assertTrue(edge.canTraverse(TraverseMode.WALK));
  }

  /**
   * Create an edge. If twoWay, create two edges (back and forth).
   *
   * @param back true if this is a reverse edge
   */
  private StreetEdge edge(StreetVertex vA, StreetVertex vB, double length, boolean back) {
    var labelA = vA.getLabel();
    var labelB = vB.getLabel();
    String name = String.format("%s_%s", labelA, labelB);
    Coordinate[] coords = new Coordinate[2];
    coords[0] = vA.getCoordinate();
    coords[1] = vB.getCoordinate();
    LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

    StreetTraversalPermission perm = StreetTraversalPermission.ALL;
    return new StreetEdgeBuilder<>()
      .withFromVertex(vA)
      .withToVertex(vB)
      .withGeometry(geom)
      .withName(name)
      .withMeterLength(length)
      .withPermission(perm)
      .withBack(back)
      .buildAndConnect();
  }
}
