package org.opentripplanner.street.model.vertex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;

public class IntersectionVertexTest {

  private StreetEdge fromEdge;
  private StreetEdge straightAheadEdge;

  @BeforeEach
  public void before() {
    // Graph for a fictional grid city with turn restrictions
    StreetVertex maple1 = vertex("maple_1st", 2.0, 2.0);
    StreetVertex maple2 = vertex("maple_2nd", 1.0, 2.0);
    StreetVertex maple3 = vertex("maple_3rd", 0.0, 2.0);

    StreetVertex main1 = vertex("main_1st", 2.0, 1.0);
    StreetVertex main2 = vertex("main_2nd", 1.0, 1.0);
    StreetVertex main3 = vertex("main_3rd", 0.0, 1.0);

    StreetVertex broad1 = vertex("broad_1st", 2.0, 0.0);
    StreetVertex broad2 = vertex("broad_2nd", 1.0, 0.0);
    StreetVertex broad3 = vertex("broad_3rd", 0.0, 0.0);

    // Each block along the main streets has unit length and is one-way
    StreetEdge maple1_2 = edge(maple1, maple2, 100.0, false);
    StreetEdge maple2_3 = edge(maple2, maple3, 100.0, false);

    StreetEdge main1_2 = edge(main1, main2, 100.0, false);
    StreetEdge main2_3 = edge(main2, main3, 100.0, false);

    StreetEdge broad1_2 = edge(broad1, broad2, 100.0, false);
    StreetEdge broad2_3 = edge(broad2, broad3, 100.0, false);

    // Each cross-street connects
    StreetEdge maple_main1 = edge(maple1, main1, 50.0, false);
    StreetEdge main_broad1 = edge(main1, broad1, 100.0, false);

    StreetEdge maple_main2 = edge(maple2, main2, 50.0, false);
    StreetEdge main_broad2 = edge(main2, broad2, 50.0, false);

    StreetEdge maple_main3 = edge(maple3, main3, 100.0, false);
    StreetEdge main_broad3 = edge(main3, broad3, 100.0, false);

    this.fromEdge = maple1_2;
    this.straightAheadEdge = maple2_3;
  }

  @Test
  public void testInferredFreeFlowing() {
    IntersectionVertex iv = StreetModelForTest.intersectionVertex("vertex", 1.0, 2.0);
    assertFalse(iv.hasDrivingTrafficLight());
    assertFalse(iv.inferredFreeFlowing());
    assertEquals(0, iv.getDegreeIn());
    assertEquals(0, iv.getDegreeOut());

    iv = new LabelledIntersectionVertex("vertex", 1.0, 2.0, true, false);
    assertTrue(iv.hasDrivingTrafficLight());
    assertTrue(iv.hasCyclingTrafficLight());
    assertFalse(iv.hasWalkingTrafficLight());
    assertFalse(iv.inferredFreeFlowing());

    iv.addIncoming(fromEdge);
    assertEquals(1, iv.getDegreeIn());
    assertEquals(0, iv.getDegreeOut());
    assertFalse(iv.inferredFreeFlowing());

    iv.addOutgoing(straightAheadEdge);
    assertEquals(1, iv.getDegreeIn());
    assertEquals(1, iv.getDegreeOut());
    assertFalse(iv.inferredFreeFlowing());

    iv = StreetModelForTest.intersectionVertex("vertex", 1.0, 2.0);
    iv.addIncoming(fromEdge);
    iv.addOutgoing(straightAheadEdge);
    assertFalse(iv.hasDrivingTrafficLight());
    assertTrue(iv.inferredFreeFlowing());

    iv = new LabelledIntersectionVertex("vertex", 1.0, 2.0, false, true);
    iv.addIncoming(fromEdge);
    iv.addOutgoing(straightAheadEdge);
    assertTrue(iv.hasWalkingTrafficLight());
    assertTrue(iv.hasCyclingTrafficLight());
    assertFalse(iv.inferredFreeFlowing());
  }

  /****
   * Private Methods
   ****/

  private StreetVertex vertex(String label, double lat, double lon) {
    return StreetModelForTest.intersectionVertex(label, lat, lon);
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
